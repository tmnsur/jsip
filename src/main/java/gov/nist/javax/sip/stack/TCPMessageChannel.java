package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a stack abstraction for TCP connections. This abstracts a stream of
 * parsed messages. The SIP sipStack starts this from the main SIPStack class
 * for each connection that it accepts. It starts a message parser in its own
 * thread and talks to the message parser via a pipe. The message parser calls
 * back via the parseError or processMessage functions that are defined as part
 * of the SIPMessageListener interface.
 */
public class TCPMessageChannel extends ConnectionOrientedMessageChannel {
	private static final Logger logger = Logger.getLogger(TCPMessageChannel.class.getName());

	private static final String BASED_ON_THE_NEW_SOCKET_FOR_THE_INCOMING = " based on the new socket for incoming: {0}";

	protected OutputStream myClientOutputStream;

	protected TCPMessageChannel(SIPTransactionStack sipStack) {
		super(sipStack);
	}

	/**
	 * Constructor - gets called from the SIPStack class with a socket on accepting
	 * a new client. All the processing of the message is done here with the
	 * sipStack being freed up to handle new connections. The sock input is the
	 * socket that is returned from the accept. Global data that is shared by all
	 * threads is accessible in the Server structure.
	 *
	 * @param sock     Socket from which to read and write messages. The socket is
	 *                 already connected (was created as a result of an accept).
	 *
	 * @param sipStack Ptr to SIP Stack
	 */

	protected TCPMessageChannel(Socket sock, SIPTransactionStack sipStack, TCPMessageProcessor msgProcessor,
			String threadName) throws IOException {

		super(sipStack);

		logger.log(Level.FINEST, "creating new TCPMessageChannel");

		mySock = sock;
		peerAddress = mySock.getInetAddress();
		myAddress = msgProcessor.getIpAddress().getHostAddress();
		myClientInputStream = mySock.getInputStream();
		myClientOutputStream = mySock.getOutputStream();

		mythread = new Thread(this);

		mythread.setDaemon(true);
		mythread.setName(threadName);

		this.peerPort = mySock.getPort();
		this.key = MessageChannel.getKey(peerAddress, peerPort, "TCP");

		this.myPort = msgProcessor.getPort();

		super.messageProcessor = msgProcessor;

		// Can drop this after response is sent potentially.
		mythread.start();
	}

	/**
	 * Constructor - connects to the given inet address. A thread was being
	 * uncessarily created.
	 *
	 * @param inetAddr inet address to connect to.
	 * @param sipStack is the sip sipStack from which we are created.
	 * @throws IOException if we cannot connect.
	 */
	protected TCPMessageChannel(InetAddress inetAddr, int port, SIPTransactionStack sipStack,
			TCPMessageProcessor messageProcessor) throws IOException {
		super(sipStack);

		logger.log(Level.FINEST, "creating new TCPMessageChannel ");

		this.peerAddress = inetAddr;
		this.peerPort = port;
		this.myPort = messageProcessor.getPort();
		this.peerProtocol = "TCP";
		this.myAddress = messageProcessor.getIpAddress().getHostAddress();
		this.key = MessageChannel.getKey(peerAddress, peerPort, "TCP");

		super.messageProcessor = messageProcessor;
	}

	/**
	 * Close the message channel.
	 */
	@Override
	public void close(boolean removeSocket, boolean stopKeepAliveTask) {
		isRunning = false;
		/*
		 * we need to close everything because the socket may be closed by the other end like in LB scenarios sending
		 * OPTIONS and killing the socket after it gets the response
		 */
		if(mySock != null) {
			logger.log(Level.FINEST, "Closing socket: {0}", key);

			try {
				mySock.close();
				mySock = null;
			} catch(IOException ex) {
				logger.log(Level.FINEST, "Error closing socket", ex);
			}
		}

		if(myParser != null) {
			logger.log(Level.FINEST, "Closing my parser: {0}", myParser);

			myParser.close();
		}

		// no need to close myClientInputStream since myParser.close() above will do it
		if(myClientOutputStream != null) {
			logger.log(Level.FINEST, "Closing client output stream: {0}", myClientOutputStream);

			try {
				myClientOutputStream.close();
			} catch(IOException ex) {
				logger.log(Level.FINEST, "Error closing client output stream", ex);
			}
		}

		if(removeSocket) {
			// remove the "TCP:" part of the key to cleanup the ioHandler hash map
			String ioHandlerKey = key.substring(4);

			logger.log(Level.FINEST, "Closing TCP socket: {0}", ioHandlerKey);

			// Issue 358 : remove socket and semaphore on close to avoid leaking
			sipStack.ioHandler.removeSocket(ioHandlerKey);

			logger.log(Level.FINEST, "Closing message Channel (key: {0}): {1}", new Object[] { key, this });
		} else {
			logger.log(Level.FINEST, "not removing socket key from the cached map since it has already been updated by"
					+ " the iohandler.sendBytes: {0}", key.substring(4));
		}

		if(stopKeepAliveTask) {
			cancelPingKeepAliveTimeoutTaskIfStarted();
		}
	}

	/**
	 * get the transport string.
	 *
	 * @return "TCP" in this case.
	 */
	@Override
	public String getTransport() {
		return "TCP";
	}

	/**
	 * Send message to whoever is connected to us. Uses the topmost via address to
	 * send to.
	 *
	 * @param msg      is the message to send.
	 * @param isClient
	 */
	@Override
	protected synchronized void sendMessage(byte[] msg, boolean isClient) throws IOException {
		logger.log(Level.FINEST, "sendMessage isClient: {0}", isClient);

		Socket sock = null;
		IOException problem = null;
		// try to prevent at least the worst thread safety issues by using a local variable ...
		Socket mySockLocal = mySock;

		try {
			sock = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), this.peerAddress,
					this.peerPort, this.peerProtocol, msg, isClient, this);
		} catch (IOException any) {
			problem = any;

			logger.log(Level.WARNING, "Failed to connect {0}:{1} but trying the advertised port: {2} if it's different"
					+ " than the port we just failed on", new Object[] { this.peerAddress, this.peerPort,
							this.peerPortAdvertisedInHeaders });
		}

		/*
		 * http://java.net/jira/browse/JSIP-362 If we couldn't connect to the host, try the advertised host
		 * and port as fail safe
		 */
		if(null != problem) {
			if(peerAddressAdvertisedInHeaders != null && peerPortAdvertisedInHeaders > 0) {
				logger.log(Level.WARNING, "Couldn''t connect to peerAddress: {0}, peerPort: {1}, key: {2} retrying on"
						+ " peerPortAdvertisedInHeaders: {3}", new Object[] { peerAddress, peerPort, key,
								peerPortAdvertisedInHeaders });

				InetAddress address = InetAddress.getByName(peerAddressAdvertisedInHeaders);

				sock = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), address,
						this.peerPortAdvertisedInHeaders, this.peerProtocol, msg, isClient, this);

				this.peerPort = this.peerPortAdvertisedInHeaders;
				this.peerAddress = address;
				this.key = MessageChannel.getKey(peerAddress, peerPort, "TCP");

				logger.log(Level.WARNING, "retry suceeded to peerAddress: {0}, peerPortAdvertisedInHeaders: {1},"
						+ " key: {2}", new Object[] { peerAddress, peerPortAdvertisedInHeaders, key });
			} else {
				// throw the original exception we had from the first attempt
				throw problem;
			}
		}

		/*
		 * Created a new socket so close the old one and stick the new one in its place but don't do this if it is
		 * a datagram socket. (could have replied via udp but received via tcp!).
		 */
		if(sock != mySockLocal && sock != null) {
			if(mySockLocal != null) {
				logger.log(Level.WARNING, "Old socket different than new socket on channel: {0}", key);
				logger.log(Level.WARNING, "Old socket local ip address: {0}", mySock.getLocalSocketAddress());
				logger.log(Level.WARNING, "Old socket remote ip address: {0}", mySock.getRemoteSocketAddress());
				logger.log(Level.WARNING, "New socket local ip address: {0}", sock.getLocalSocketAddress());
				logger.log(Level.WARNING, "New socket remote ip address: {0}", sock.getRemoteSocketAddress());

				close(false, false);
			}

			if(problem == null) {
				if(mySockLocal != null) {
					logger.log(Level.WARNING, "There was no exception for the retry mechanism so creating a new thread"
							+ BASED_ON_THE_NEW_SOCKET_FOR_THE_INCOMING, key);
				}

				// NOTE: need to consider refactoring the whole socket handling with respect to thread safety
				if(mySockLocal == mySock) {
					// still not thread safe :-( but what else to do?
					mySock = sock;

					this.myClientInputStream = mySock.getInputStream();
					this.myClientOutputStream = mySock.getOutputStream();

					Thread thread = new Thread(this);

					thread.setDaemon(true);
					thread.setName("TCPMessageChannelThread");

					thread.start();
				}
			} else {
				logger.log(Level.WARNING, "There was an exception for the retry mechanism so not creating a new thread"
						+ BASED_ON_THE_NEW_SOCKET_FOR_THE_INCOMING, key);

				mySock = sock;
			}
		}
	}

	/**
	 * Send a message to a specified address.
	 *
	 * @param message         Pre-formatted message to send.
	 * @param receiverAddress Address to send it to.
	 * @param receiverPort    Receiver port.
	 * @throws IOException If there is a problem connecting or sending.
	 */
	@Override
	public synchronized void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		if(message == null || receiverAddress == null) {
			throw new IllegalArgumentException("Null argument");
		}

		if(peerPortAdvertisedInHeaders <= 0) {
			logger.log(Level.FINEST, "receiver port: {0} for this channel: {1}, key: {2}",
					new Object[] { receiverPort, this, key });

			// if port is 0 we assume the default port for TCP
			if(receiverPort <= 0) {
				this.peerPortAdvertisedInHeaders = 5060;
			} else {
				this.peerPortAdvertisedInHeaders = receiverPort;
			}

			logger.log(Level.FINEST, "2.Storing peerPortAdvertisedInHeaders: {0}, for this channel: {1}, key: {2}",
					new Object[] { peerPortAdvertisedInHeaders, this, key });
		}

		Socket sock = null;
		IOException problem = null;
		try {
			sock = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), receiverAddress,
					receiverPort, "TCP", message, retry, this);
		} catch(IOException any) {
			problem = any;

			logger.log(Level.WARNING, "Failed to connect {0}:{1} but trying the advertised port: {2} if it''s different"
					+ " than the port we just failed on", new Object[] { this.peerAddress, receiverPort,
							this.peerPortAdvertisedInHeaders });

			logger.log(Level.SEVERE, "Error is", any);
		}

		/*
		 * http://java.net/jira/browse/JSIP-362 If we couldn't connect to the host, try the advertised host:port
		 * as fail safe
		 */
		if(null != problem) {
			if(peerAddressAdvertisedInHeaders != null && peerPortAdvertisedInHeaders > 0) {
				logger.log(Level.WARNING, "Couldn''t connect to receiverAddress: {0} receiverPort: {1}, key: {2},"
						+ "retrying on peerPortAdvertisedInHeaders: {3}", new Object[] { receiverAddress, receiverPort,
								key, peerPortAdvertisedInHeaders });

				InetAddress address = InetAddress.getByName(peerAddressAdvertisedInHeaders);

				sock = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), address,
						this.peerPortAdvertisedInHeaders, "TCP", message, retry, this);

				this.peerPort = this.peerPortAdvertisedInHeaders;
				this.peerAddress = address;
				this.key = MessageChannel.getKey(peerAddress, peerPort, "TCP");

				logger.log(Level.WARNING, "retry suceeded to peerAddress: {0}, peerPort: {1}, key: {2}",
						new Object[] { peerAddress, peerPort, key });
			} else {
				// throw the original exception we had from the first attempt
				throw problem;
			}
		}

		if(sock != mySock && sock != null) {
			if(mySock != null) {
				logger.log(Level.WARNING, "Old socket different than new socket on channel: {0}", key);
				logger.log(Level.WARNING, "Old socket local ip address: {0}", mySock.getLocalSocketAddress());
				logger.log(Level.WARNING, "Old socket remote ip address: {0}", mySock.getRemoteSocketAddress());
				logger.log(Level.WARNING, "New socket local ip address: {0}", sock.getLocalSocketAddress());
				logger.log(Level.WARNING, "New socket remote ip address: {0}", sock.getRemoteSocketAddress());

				close(false, false);
			}

			if(problem == null) {
				if(mySock != null) {
					logger.log(Level.WARNING, "There was no exception for the retry mechanism so creating a new thread"
							+ BASED_ON_THE_NEW_SOCKET_FOR_THE_INCOMING, key);
				}

				mySock = sock;

				this.myClientInputStream = mySock.getInputStream();
				this.myClientOutputStream = mySock.getOutputStream();

				// start a new reader on this end of the pipe.
				Thread mythread = new Thread(this);

				mythread.setDaemon(true);
				mythread.setName("TCPMessageChannelThread");

				mythread.start();
			} else {
				logger.log(Level.WARNING, "There was an exception for the retry mechanism so not creating a new thread"
						+ BASED_ON_THE_NEW_SOCKET_FOR_THE_INCOMING, key);

				mySock = sock;
			}
		}
	}

	/**
	 * Exception processor for exceptions detected from the parser. (This is invoked
	 * by the parser when an error is detected).
	 *
	 * @param sipMessage -- the message that incurred the error.
	 * @param ex         -- parse exception detected by the parser.
	 * @param header     -- header that caused the error.
	 * @throws ParseException Thrown if we want to reject the message.
	 */
	public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message)
			throws ParseException {
		logger.log(Level.SEVERE, ex.getMessage(), ex);

		// Log the bad message for later reference.
		if ((hdrClass != null) && (hdrClass.equals(From.class) || hdrClass.equals(To.class)
				|| hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class)
				|| hdrClass.equals(ContentLength.class) || hdrClass.equals(RequestLine.class)
				|| hdrClass.equals(StatusLine.class))) {
			logger.log(Level.FINEST, "Encountered Bad Message\n{0}", sipMessage);

			// send a 400 response for requests (except ACK) Currently only UDP,
			String msgString = sipMessage.toString();
			if(!msgString.startsWith("SIP/") && !msgString.startsWith("ACK ") && mySock != null) {
				logger.log(Level.SEVERE, "Malformed mandatory headers: closing socket!: {0}", mySock);

				try {
					mySock.close();

				} catch(IOException ie) {
					logger.log(Level.SEVERE, "Exception while closing socket!: {0}:{1}", new Object[] { mySock,
							ie });
				}
			}

			throw ex;
		} else {
			sipMessage.addUnparsed(header);
		}
	}

	/**
	 * Equals predicate.
	 *
	 * @param other is the other object to compare ourselves to for equals
	 */
	@Override
	public boolean equals(Object other) {
		if(null == other) {
			return false;
		}

		if(!this.getClass().equals(other.getClass())) {
			return false;
		}

		return this.mySock == ((TCPMessageChannel) other).mySock;
	}

	/**
	 * TCP Is not a secure protocol.
	 */
	@Override
	public boolean isSecure() {
		return false;
	}
}
