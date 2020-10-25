package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.IOExceptionEventExt;
import gov.nist.javax.sip.IOExceptionEventExt.Reason;
import gov.nist.javax.sip.SipListenerExt;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.Pipeline;
import gov.nist.javax.sip.parser.PipelinedMsgParser;
import gov.nist.javax.sip.parser.SIPMessageListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.ListeningPoint;
import javax.sip.SipListener;
import javax.sip.address.Hop;
import javax.sip.message.Response;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public abstract class ConnectionOrientedMessageChannel extends MessageChannel
		implements SIPMessageListener, Runnable, RawMessageChannel {

	private static final Logger logger = Logger.getLogger(ConnectionOrientedMessageChannel.class.getName());

	protected SIPTransactionStack sipStack;
	protected Socket mySock;
	protected PipelinedMsgParser myParser;
	protected String key;
	// just to pass to thread.
	protected InputStream myClientInputStream;
	// Set here on initialization to avoid thread leak. See issue 266
	protected boolean isRunning = true;
	protected boolean isCached;
	protected Thread mythread;
	protected String myAddress;
	protected int myPort;
	protected InetAddress peerAddress;
	// This is the port and address that we will find in the headers of the messages from the peer
	protected int peerPortAdvertisedInHeaders = -1;
	protected String peerAddressAdvertisedInHeaders;
	protected int peerPort;
	protected String peerProtocol;
	private volatile long lastKeepAliveReceivedTime;
	private SIPStackTimerTask pingKeepAliveTimeoutTask;
	private Semaphore keepAliveSemaphore;

	private long keepAliveTimeout;

	public ConnectionOrientedMessageChannel(SIPTransactionStack sipStack) {
		this.sipStack = sipStack;
		this.keepAliveTimeout = sipStack.getReliableConnectionKeepAliveTimeout();

		if(keepAliveTimeout > 0) {
			keepAliveSemaphore = new Semaphore(1);
		}
	}

	/**
	 * Returns "true" as this is a reliable transport.
	 */
	public boolean isReliable() {
		return true;
	}

	/**
	 * Close the message channel.
	 */
	public void close() {
		close(true, true);
	}

	protected abstract void close(boolean removeSocket, boolean stopKeepAliveTask);

	/**
	 * Get my SIP Stack.
	 *
	 * @return The SIP Stack for this message channel.
	 */
	public SIPTransactionStack getSIPStack() {
		return sipStack;
	}

	/**
	 * get the address of the client that sent the data to us.
	 *
	 * @return Address of the client that sent us data that resulted in this channel
	 *         being created.
	 */
	public String getPeerAddress() {
		if (peerAddress != null) {
			return peerAddress.getHostAddress();
		} else
			return getHost();
	}

	protected InetAddress getPeerInetAddress() {
		return peerAddress;
	}

	public String getPeerProtocol() {
		return this.peerProtocol;
	}

	/**
	 * Return a formatted message to the client. We try to re-connect with the peer
	 * on the other end if possible.
	 *
	 * @param sipMessage Message to send.
	 * @throws IOException If there is an error sending the message
	 */
	public void sendMessage(final SIPMessage sipMessage) throws IOException {
		if(!sipMessage.isNullRequest()) {
			logger.log(Level.FINEST, "sendMessage:: {0} cseq method = {1}",
					new Object[] {sipMessage.getFirstLine(), sipMessage.getCSeq().getMethod()});
		}

		for(MessageProcessor messageProcessor : getSIPStack().getMessageProcessors()) {
			if(messageProcessor.getIpAddress().getHostAddress().equals(this.getPeerAddress())
					&& messageProcessor.getPort() == this.getPeerPort()
					&& messageProcessor.getTransport().equalsIgnoreCase(this.getPeerProtocol())) {
				Runnable processMessageTask = new Runnable() {
					public void run() {
						try {
							processMessage((SIPMessage) sipMessage.clone());
						} catch(Exception ex) {
							logger.log(Level.SEVERE, "Error self routing message cause by", ex);
						}
					}
				};
				getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);

				logger.log(Level.FINEST, "Self routing message");

				return;
			}
		}

		byte[] msg = sipMessage.encodeAsBytes(this.getTransport());
		long time = System.currentTimeMillis();

		// need to store the peerPortAdvertisedInHeaders in case the response has an
		// rport (ephemeral) that failed to retry on the regular via port
		// for responses, no need to store anything for subsequent requests.
		if(peerPortAdvertisedInHeaders <= 0 && sipMessage instanceof SIPResponse) {
			SIPResponse sipResponse = (SIPResponse) sipMessage;
			Via via = sipResponse.getTopmostVia();
			if(via.getRPort() > 0) {
				if(via.getPort() <= 0) {
					// if port is 0 we assume the default port for TCP
					this.peerPortAdvertisedInHeaders = 5060;
				} else {
					this.peerPortAdvertisedInHeaders = via.getPort();
				}

				logger.log(Level.FINEST, "1.Storing peerPortAdvertisedInHeaders = {0} for via port = {1}"
						+ " via rport = {2} and peer port = {3} for this channel {4} key {5}",
								new Object[] {peerPortAdvertisedInHeaders, via.getPort(), via.getRPort(), peerPort,
										this, key});
			}
		}

		// JvB: also retry for responses, if the connection is gone we should
		// try to reconnect
		this.sendMessage(msg, sipMessage instanceof SIPRequest);

		// message was sent without any exception so let's set set port and
		// address before we feed it to the logger
		sipMessage.setRemoteAddress(this.peerAddress);
		sipMessage.setRemotePort(this.peerPort);
		sipMessage.setLocalAddress(this.getMessageProcessor().getIpAddress());
		sipMessage.setLocalPort(this.getPort());

		logMessage(sipMessage, peerAddress, peerPort, time);
	}

	public void processMessage(SIPMessage sipMessage, InetAddress address) {
		this.peerAddress = address;

		try {
			processMessage(sipMessage);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "ERROR processing self routing", e);
		}
	}

	/**
	 * Gets invoked by the parser as a callback on successful message parsing (i.e.
	 * no parser errors).
	 *
	 * @param sipMessage Message to process (this calls the application for
	 *                   processing the message).
	 *
	 *                   Jvb: note that this code is identical to TCPMessageChannel,
	 *                   refactor some day
	 */
	public void processMessage(SIPMessage sipMessage) throws Exception {
		if (sipMessage.getFrom() == null || sipMessage.getTo() == null || sipMessage.getCallId() == null
				|| sipMessage.getCSeq() == null || sipMessage.getViaHeaders() == null) {
			String badmsg = sipMessage.encode();

			logger.log(Level.SEVERE, "bad message {0} >>> Dropped Bad Msg", badmsg);

			return;
		}

		sipMessage.setRemoteAddress(this.peerAddress);
		sipMessage.setRemotePort(this.getPeerPort());
		sipMessage.setLocalAddress(this.getMessageProcessor().getIpAddress());
		sipMessage.setLocalPort(this.getPort());

		ViaList viaList = sipMessage.getViaHeaders();
		// For a request
		// first via header tells where the message is coming from.
		// For response, this has already been recorded in the outgoing
		// message.
		if (sipMessage instanceof SIPRequest) {
			Via v = (Via) viaList.getFirst();
			// the peer address and tag it appropriately.
			Hop hop = sipStack.addressResolver.resolveAddress(v.getHop());

			this.peerProtocol = v.getTransport();

			int hopPort = v.getPort();

			logger.log(Level.FINEST, "hop port = {0} for request {1} for this channel {2} key {3}",
					new Object[] {hopPort, sipMessage, this, key});

			if(hopPort <= 0) {
				// if port is 0 we assume the default port for TCP
				this.peerPortAdvertisedInHeaders = 5060;
			} else {
				this.peerPortAdvertisedInHeaders = hopPort;
			}

			logger.log(Level.FINEST, "3.Storing peerPortAdvertisedInHeaders = {0} for this channel {1} key {2}",
					new Object[] {peerPortAdvertisedInHeaders, this, key});

			// may be needed to reconnect, when diff than peer address
			if(peerAddressAdvertisedInHeaders == null) {
				peerAddressAdvertisedInHeaders = hop.getHost();

				logger.log(Level.FINEST, "3.Storing peerAddressAdvertisedInHeaders = {0} for this channel {1}"
						+ " key {2}", new Object[] {peerAddressAdvertisedInHeaders, this, key});
			}

			try {
				// selfrouting makes socket = null https://jain-sip.dev.java.net/issues/show_bug.cgi?id=297
				if(mySock != null) { 
					this.peerAddress = mySock.getInetAddress();
				}

				/* Check to see if the received parameter matches the peer address and tag it appropriately.
				 * don't do this. It is both costly and incorrect Must set received also when it is a FQDN,
				 * regardless whether it resolves to the correct IP address if sender added 'rport',
				 * must always set received
				 */
				if(v.hasParameter(Via.RPORT) || !hop.getHost().equals(this.peerAddress.getHostAddress())) {
					v.setParameter(Via.RECEIVED, this.peerAddress.getHostAddress());
				}

				// technically, may only do this when Via already contains rport
				v.setParameter(Via.RPORT, Integer.toString(this.peerPort));
			} catch(ParseException ex) {
				InternalErrorHandler.handleException(ex);
			}

			/*
			 * Use this for outgoing messages as well. self routing makes mySock=null
			 * https://jain-sip.dev.java.net/issues/show_bug.cgi?id=297
			 */
			if(!this.isCached && mySock != null) {
				this.isCached = true;
				int remotePort = ((java.net.InetSocketAddress) mySock.getRemoteSocketAddress()).getPort();

				String key = IOHandler.makeKey(mySock.getInetAddress(), remotePort);

				if(this.messageProcessor instanceof NioTcpMessageProcessor) {
					// https://java.net/jira/browse/JSIP-475 don't use iohandler in case of NIO
					// communications of the socket will leak in the iohandler sockettable
					((NioTcpMessageProcessor) this.messageProcessor).nioHandler.putSocket(key, mySock.getChannel());
				} else {
					sipStack.ioHandler.putSocket(key, mySock);
				}

				// since it can close the socket it needs to be after the mySock usage otherwise
				// it the socket will be disconnected and NPE will be thrown in some edge cases
				((ConnectionOrientedMessageProcessor) this.messageProcessor).cacheMessageChannel(this);
			}
		}

		// Foreach part of the request header, fetch it and process it

		long receptionTime = System.currentTimeMillis();

		if(sipMessage instanceof SIPRequest) {
			// This is a request - process the request.
			SIPRequest sipRequest = (SIPRequest) sipMessage;
			// Create a new sever side request processor for this
			// message and let it handle the rest.

			logger.log(Level.FINEST, "----Processing Message---");
			logger.log(Level.FINEST, "sipMessage: {0}, peerHostPort: {1}, hostAddress: {2}, port: {3},"
					+ "receptionTime: {4}", new Object[] { sipMessage, this.getPeerHostPort(),
					this.messageProcessor.getIpAddress().getHostAddress(), this.messageProcessor.getPort(),
							receptionTime});

			// Check for reasonable size - reject message if it is too long.
			if(sipStack.getMaxMessageSize() > 0
					&& sipRequest.getSize() + (sipRequest.getContentLength() == null ? 0
							: sipRequest.getContentLength().getContentLength()) > sipStack.getMaxMessageSize()) {
				SIPResponse sipResponse = sipRequest.createResponse(Response.MESSAGE_TOO_LARGE);

				byte[] resp = sipResponse.encodeAsBytes(this.getTransport());

				this.sendMessage(resp, false);

				throw new Exception("Message size exceeded");
			}

			String sipVersion = ((SIPRequest) sipMessage).getRequestLine().getSipVersion();
			if(!sipVersion.equals("SIP/2.0")) {
				SIPResponse versionNotSupported = ((SIPRequest) sipMessage)
						.createResponse(Response.VERSION_NOT_SUPPORTED, "Bad SIP version " + sipVersion);

				this.sendMessage(versionNotSupported.encodeAsBytes(this.getTransport()), false);

				throw new Exception("Bad version ");
			}

			String method = ((SIPRequest) sipMessage).getMethod();
			String cseqMethod = ((SIPRequest) sipMessage).getCSeqHeader().getMethod();

			if (!method.equalsIgnoreCase(cseqMethod)) {
				SIPResponse sipResponse = sipRequest.createResponse(Response.BAD_REQUEST);

				byte[] resp = sipResponse.encodeAsBytes(this.getTransport());
				this.sendMessage(resp, false);

				throw new Exception("Bad CSeq method" + sipMessage + " method " + method);
			}

			// Stack could not create a new server request interface. maybe not enough resources.
			ServerRequestInterface sipServerRequest = sipStack.newSIPServerRequest(sipRequest, this);

			if(sipServerRequest != null) {
				try {
					sipServerRequest.processRequest(sipRequest, this);
				} finally {
					if(sipServerRequest instanceof SIPTransaction) {
						SIPServerTransaction sipServerTx = (SIPServerTransaction) sipServerRequest;
						if(!sipServerTx.passToListener()) {
							((SIPTransaction) sipServerRequest).releaseSem();
						}
					}
				}
			} else {
				// Allow message valves to nullify messages without error
				if(sipStack.sipMessageValve == null) {
					SIPResponse response = sipRequest.createResponse(Response.SERVICE_UNAVAILABLE);

					RetryAfter retryAfter = new RetryAfter();

					// Be a good citizen and send a decent response code back.
					try {
						retryAfter.setRetryAfter((int) (10 * (Math.random())));
						response.setHeader(retryAfter);
						this.sendMessage(response);
					} catch (Exception e) {
						// IGNore
					}

					logger.log(Level.WARNING, "Dropping message -- could not acquire semaphore");
				}
			}
		} else {
			SIPResponse sipResponse = (SIPResponse) sipMessage;
			try {
				sipResponse.checkHeaders();
			} catch (ParseException ex) {
				logger.log(Level.SEVERE, "Dropping Badly formatted response message >>> {0}", sipResponse);

				return;
			}

			// This is a response message - process it.
			// Check the size of the response.
			// If it is too large dump it silently.
			if(sipStack.getMaxMessageSize() > 0
					&& sipResponse.getSize() + (sipResponse.getContentLength() == null ? 0
							: sipResponse.getContentLength().getContentLength()) > sipStack.getMaxMessageSize()) {
				logger.log(Level.FINEST, "Message size exceeded");

				return;
			}

			ServerResponseInterface sipServerResponse = sipStack.newSIPServerResponse(sipResponse, this);
			if(sipServerResponse != null) {
				try {
					if(sipServerResponse instanceof SIPClientTransaction
							&& !((SIPClientTransaction) sipServerResponse).checkFromTag(sipResponse)) {
						logger.log(Level.SEVERE, "Dropping response message with invalid tag >>> {0}", sipResponse);

						return;
					}

					sipServerResponse.processResponse(sipResponse, this);
				} finally {
					if(sipServerResponse instanceof SIPTransaction
							&& !((SIPTransaction) sipServerResponse).passToListener()) {
						/*
						 * Note that the semaphore is released in event scanner if the request is actually
						 * processed by the Listener.
						 */
						((SIPTransaction) sipServerResponse).releaseSem();
					}
				}
			} else {
				logger.log(Level.WARNING, "Application is blocked -- could not acquire semaphore -- dropping response");
			}
		}
	}

	/**
	 * This gets invoked when thread.start is called from the constructor.
	 * Implements a message loop - reading the tcp connection and processing
	 * messages until we are done or the other end has closed.
	 */
	public void run() {
		Pipeline hispipe = null;
		// Create a pipeline to connect to our message parser.
		hispipe = new Pipeline(myClientInputStream, sipStack.readTimeout, ((SIPTransactionStack) sipStack).getTimer());
		// Create a pipelined message parser to read and parse
		// messages that we write out to him.
		myParser = new PipelinedMsgParser(sipStack, this, hispipe, this.sipStack.getMaxMessageSize());
		// Start running the parser thread.
		myParser.processInput();
		// bug fix by Emmanuel Proulx
		int bufferSize = 4096;
		((ConnectionOrientedMessageProcessor) this.messageProcessor).useCount++;
		this.isRunning = true;
		try {
			while (true) {
				try {
					byte[] msg = new byte[bufferSize];
					int nbytes = myClientInputStream.read(msg, 0, bufferSize);
					// no more bytes to read...
					if (nbytes == -1) {
						hispipe.write("\r\n".getBytes("UTF-8")); // send \r\n to allow the pipe to wake up
						try {
							if (sipStack.maxConnections != -1) {
								synchronized (messageProcessor) {
									((ConnectionOrientedMessageProcessor) this.messageProcessor).nConnections--;
									messageProcessor.notify();
								}
							}
							hispipe.close();
							close();
						} catch (IOException ioex) {
						}
						return;
					}

					hispipe.write(msg, 0, nbytes);

				} catch (IOException ex) {
					// Terminate the message.
					try {
						hispipe.write("\r\n\r\n".getBytes("UTF-8"));
					} catch (Exception e) {
						logger.log(Level.FINEST, "silently ignoring exception", e);
					}

					try {
						logger.log(Level.FINEST, "IOException closing sock", ex);

						try {
							if(sipStack.maxConnections != -1) {
								synchronized(messageProcessor) {
									((ConnectionOrientedMessageProcessor) this.messageProcessor).nConnections--;
									messageProcessor.notify();
								}
							}
							close();
							hispipe.close();
						} catch (IOException ioex) {
							logger.log(Level.FINEST, "silently ignoring exception", ioex);
						}
					} catch (Exception ex1) {
						logger.log(Level.FINEST, "silently ignoring exception", ex1);
					}
					return;
				} catch(Exception ex) {
					InternalErrorHandler.handleException(ex);
				}
			}
		} finally {
			this.isRunning = false;
			((ConnectionOrientedMessageProcessor) this.messageProcessor).remove(this);
			((ConnectionOrientedMessageProcessor) this.messageProcessor).useCount--;
			// parser could be null if the socket was closed by the remote end already
			if(myParser != null) {
				myParser.close();
			}
		}

	}

	protected void uncache() {
		if (isCached && !isRunning) {
			((ConnectionOrientedMessageProcessor) this.messageProcessor).remove(this);
		}
	}

	/**
	 * Get an identifying key. This key is used to cache the connection and re-use
	 * it if necessary.
	 */
	public String getKey() {
		if (this.key != null) {
			return this.key;
		} else {
			this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, getTransport());
			return this.key;
		}
	}

	/**
	 * Get the host to assign to outgoing messages.
	 *
	 * @return the host to assign to the via header.
	 */
	public String getViaHost() {
		return myAddress;
	}

	/**
	 * Get the port for outgoing messages sent from the channel.
	 *
	 * @return the port to assign to the via header.
	 */
	public int getViaPort() {
		return myPort;
	}

	/**
	 * Get the port of the peer to whom we are sending messages.
	 *
	 * @return the peer port.
	 */
	public int getPeerPort() {
		return peerPort;
	}

	public int getPeerPacketSourcePort() {
		return this.peerPort;
	}

	public InetAddress getPeerPacketSourceAddress() {
		return this.peerAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.parser.SIPMessageListener#sendSingleCLRF()
	 */
	public void sendSingleCRLF() throws Exception {
		lastKeepAliveReceivedTime = System.currentTimeMillis();

		if (mySock != null && !mySock.isClosed()) {
			sendMessage("\r\n".getBytes("UTF-8"), false);
		}

		synchronized (this) {
			if (isRunning) {
				if (keepAliveTimeout > 0) {
					rescheduleKeepAliveTimeout(keepAliveTimeout);
				}
			}
		}
	}

	public void cancelPingKeepAliveTimeoutTaskIfStarted() {
		if (pingKeepAliveTimeoutTask != null && pingKeepAliveTimeoutTask.getSipTimerTask() != null) {
			try {
				keepAliveSemaphore.acquire();
			} catch(InterruptedException e) {
				logger.log(Level.SEVERE, "Couldn't acquire keepAliveSemaphore", e);
				return;
			}

			try {
				logger.log(Level.FINEST, "~~~ cancelPingKeepAliveTimeoutTaskIfStarted for MessageChannel(key={0}),"
						+ " clientAddress={1}, clientPort={2}, timeout={3})",
								new Object[] {key, peerAddress, peerPort, keepAliveTimeout});

				sipStack.getTimer().cancel(pingKeepAliveTimeoutTask);
			} finally {
				keepAliveSemaphore.release();
			}
		}
	}

	public void setKeepAliveTimeout(long keepAliveTimeout) {
		if (keepAliveTimeout < 0) {
			cancelPingKeepAliveTimeoutTaskIfStarted();
		}
		if (keepAliveTimeout == 0) {
			keepAliveTimeout = messageProcessor.getSIPStack().getReliableConnectionKeepAliveTimeout();
		}

		logger.log(Level.FINEST, "~~~ setKeepAliveTimeout for MessageChannel(key={0}), clientAddress={1},"
				+ " clientPort={2}, timeout={3})", new Object[] {key, peerAddress, peerPort, keepAliveTimeout});

		this.keepAliveTimeout = keepAliveTimeout;
		if(keepAliveSemaphore == null) {
			keepAliveSemaphore = new Semaphore(1);
		}

		boolean isKeepAliveTimeoutTaskScheduled = pingKeepAliveTimeoutTask != null;
		if(isKeepAliveTimeoutTaskScheduled && keepAliveTimeout > 0) {
			rescheduleKeepAliveTimeout(keepAliveTimeout);
		}
	}

	public long getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	public void rescheduleKeepAliveTimeout(long newKeepAliveTimeout) {
		logger.log(Level.FINEST, "~~~ rescheduleKeepAliveTimeout for MessageChannel(key={0}), clientAddress={1}"
				+ ", clientPort={2}, timeout={3}): newKeepAliveTimeout={4}",
						new Object[] {key, peerAddress, peerPort, keepAliveTimeout,
								newKeepAliveTimeout == Long.MAX_VALUE ? "Long.MAX_VALUE" : newKeepAliveTimeout});

		try {
			keepAliveSemaphore.acquire();
		} catch(InterruptedException e) {
			logger.log(Level.WARNING, "Couldn't acquire keepAliveSemaphore");

			return;
		}

		try {
			if(pingKeepAliveTimeoutTask == null) {
				pingKeepAliveTimeoutTask = new KeepAliveTimeoutTimerTask();

				logger.log(Level.FINEST, ", scheduling pingKeepAliveTimeoutTask to execute after {0} seconds",
						keepAliveTimeout / 1000);

				sipStack.getTimer().schedule(pingKeepAliveTimeoutTask, keepAliveTimeout);
			} else {
				logger.log(Level.FINEST, "~~~ cancelPingKeepAliveTimeout for MessageChannel(key={0}), clientAddress={1}"
						+ ", clientPort={2}, timeout={3})",
								new Object[] {key, peerAddress, peerPort, keepAliveTimeout});

				sipStack.getTimer().cancel(pingKeepAliveTimeoutTask);
				pingKeepAliveTimeoutTask = new KeepAliveTimeoutTimerTask();

				logger.log(Level.FINEST, ", scheduling pingKeepAliveTimeoutTask to execute after {0} seconds",
						keepAliveTimeout / 1000);

				sipStack.getTimer().schedule(pingKeepAliveTimeoutTask, keepAliveTimeout);
			}
		} finally {
			keepAliveSemaphore.release();
		}
	}

	class KeepAliveTimeoutTimerTask extends SIPStackTimerTask {

		public void runTask() {
			logger.log(Level.FINEST, "~~~ Starting processing of KeepAliveTimeoutEvent( {0},{1})...",
					new Object[] {peerAddress.getHostAddress(), peerPort});

			close(true, true);

			if (sipStack instanceof SipStackImpl) {
				for (Iterator<SipProviderImpl> it = ((SipStackImpl) sipStack).getSipProviders(); it.hasNext();) {
					SipProviderImpl nextProvider = (SipProviderImpl) it.next();
					SipListener sipListener = nextProvider.getSipListener();
					ListeningPoint[] listeningPoints = nextProvider.getListeningPoints();

					for(ListeningPoint listeningPoint : listeningPoints) {
						if(sipListener != null && sipListener instanceof SipListenerExt
						// making sure that we don't notify each listening point but only the one on which the timeout happened
								&& listeningPoint.getIPAddress().equalsIgnoreCase(myAddress)
								&& listeningPoint.getPort() == myPort
								&& listeningPoint.getTransport().equalsIgnoreCase(getTransport())) {
							((SipListenerExt) sipListener).processIOException(new IOExceptionEventExt(nextProvider,
									Reason.KeepAliveTimeout, myAddress, myPort, peerAddress.getHostAddress(), peerPort,
											getTransport()));
						}
					}
				}
			} else {
				SipListener sipListener = sipStack.getSipListener();
				if (sipListener instanceof SipListenerExt) {
					((SipListenerExt) sipListener)
							.processIOException(new IOExceptionEventExt(this, Reason.KeepAliveTimeout, myAddress,
									myPort, peerAddress.getHostAddress(), peerPort, getTransport()));
				}
			}
		}
	}

	protected abstract void sendMessage(byte[] msg, boolean b) throws IOException;
}
