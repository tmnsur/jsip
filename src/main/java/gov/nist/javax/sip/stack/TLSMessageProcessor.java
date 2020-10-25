package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;
import gov.nist.javax.sip.SipStackImpl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;

/**
 * Sit in a loop waiting for incoming TLS connections and start a new thread to
 * handle each new connection. This is the active object that creates new TLS
 * MessageChannels (one for each new accept socket).
 */
public class TLSMessageProcessor extends ConnectionOrientedMessageProcessor implements Runnable {
	private static final Logger logger = Logger.getLogger(TLSMessageProcessor.class.getName());

	/**
	 * Constructor.
	 * 
	 * @param ipAddress -- inet address where I am listening.
	 * @param sipStack  SIPStack structure.
	 * @param port      port where this message processor listens.
	 */
	protected TLSMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, port, "tls", sipStack);
	}

	/**
	 * Start the processor.
	 */
	public void start() throws IOException {
		Thread thread = new Thread(this);
		thread.setName("MessageProcessorThread-TLS-" + getIpAddress().getHostAddress() + '/' + getPort());
		// ISSUE 184
		thread.setPriority(sipStack.getThreadPriority());
		thread.setDaemon(true);

		this.sock = sipStack.getNetworkLayer().createSSLServerSocket(this.getPort(), 0, this.getIpAddress());
		if(sipStack.getClientAuth() == ClientAuthType.Want || sipStack.getClientAuth() == ClientAuthType.Default) {
			// we set it to true in Default case as well to keep backward compatibility and default behavior
			((SSLServerSocket) this.sock).setWantClientAuth(true);
		} else {
			((SSLServerSocket) this.sock).setWantClientAuth(false);
		}

		if(sipStack.getClientAuth() == ClientAuthType.Enabled) {
			((SSLServerSocket) this.sock).setNeedClientAuth(true);
		} else {
			((SSLServerSocket) this.sock).setNeedClientAuth(false);
		}

		((SSLServerSocket) this.sock).setUseClientMode(false);
		String[] enabledCiphers = ((SipStackImpl) sipStack).getEnabledCipherSuites();
		((SSLServerSocket) sock).setEnabledProtocols(((SipStackImpl) sipStack).getEnabledProtocols());
		((SSLServerSocket) this.sock).setEnabledCipherSuites(enabledCiphers);

		if(sipStack.getClientAuth() == ClientAuthType.Want || sipStack.getClientAuth() == ClientAuthType.Default) {
			// we set it to true in Default case as well to keep backward compatibility and default behavior
			((SSLServerSocket) this.sock).setWantClientAuth(true);
		} else {
			((SSLServerSocket) this.sock).setWantClientAuth(false);
		}

		logger.log(Level.FINEST, "SSLServerSocket want client auth {0}",
				((SSLServerSocket) this.sock).getWantClientAuth());
		logger.log(Level.FINEST, "SSLServerSocket need client auth {0}",
				((SSLServerSocket) this.sock).getNeedClientAuth());

		this.isRunning = true;

		thread.start();
	}

	/**
	 * Run method for the thread that gets created for each accept socket.
	 */
	@Override
	public void run() {
		// Accept new connections on our socket.
		while(this.isRunning) {
			Socket newsock = null;

			try {
				synchronized(this) {
					/*
					 * sipStack.maxConnections == -1 means we are willing to handle an "infinite" number of
					 * simultaneous connections (no resource limitation). This is the default behavior.
					 */
					while(sipStack.maxConnections != -1 && this.nConnections >= sipStack.maxConnections) {
						try {
							this.wait();

							if(!this.isRunning) {
								return;
							}
						} catch(InterruptedException ex) {
							break;
						}
					}

					this.nConnections++;
				}

				logger.log(Level.FINEST, "waiting to accept new connection!");

				newsock = sock.accept();
				if (sipStack.isTcpNoDelayEnabled) {
					newsock.setTcpNoDelay(true);
				}

				logger.log(Level.FINEST, "Accepting new connection!");
			} catch(SocketException ex) {
				if(this.isRunning) {
					logger.log(Level.SEVERE, "Fatal - SocketException occured while Accepting connection", ex);

					this.isRunning = false;

					break;
				}
			} catch(SSLException ex) {
				this.isRunning = false;

				logger.log(Level.SEVERE, "Fatal - SSSLException occured while Accepting connection", ex);

				break;
			} catch(IOException ex) {
				// Problem accepting connection.
				logger.log(Level.SEVERE, "Problem Accepting Connection", ex);

				continue;
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Unexpected Exception!", ex);

				continue;
			}

			// Note that for an incoming message channel, the thread is already running
			if(isRunning) {
				try {
					/*
					 * even if SocketException is thrown (could be a result of bad handshake, it's not a reason
					 * to stop execution
					 */
					TLSMessageChannel newChannel = new TLSMessageChannel(newsock, sipStack, this,
							"TLSMessageChannelThread-" + nConnections);

					logger.log(Level.FINEST, "{0} adding incoming channel {1}", new Object[] {
							Thread.currentThread(), newChannel.getKey() });

					/*
					 * https://code.google.com/p/jain-sip/issues/detail?id=14 add it only if the handshake
					 * has been completed successfully
					 */
					if(newChannel.isHandshakeCompleted()) {
						incomingMessageChannels.put(newChannel.getKey(), newChannel);
					}
				} catch(Exception ex) {
					logger.log(Level.SEVERE, "A problem occured while Accepting connection", ex);
				}
			}
		}
	}

	/**
	 * Stop the message processor.
	 */
	public synchronized void stop() {
		if(!isRunning) {
			return;
		}

		isRunning = false;
		try {
			if (sock == null) {
				logger.log(Level.FINEST, "Socket was null, perhaps not started properly");
			} else {
				sock.close();
			}
		} catch(IOException e) {
			logger.log(Level.FINEST, "silently ignoring exception", e);
		}

		Collection<ConnectionOrientedMessageChannel> en = messageChannels.values();
		for(Iterator<ConnectionOrientedMessageChannel> it = en.iterator(); it.hasNext();) {
			it.next().close();
		}

		for(Iterator<ConnectionOrientedMessageChannel> incomingMCIterator = incomingMessageChannels.values().iterator();
				incomingMCIterator.hasNext();) {
			incomingMCIterator.next().close();
		}

		this.notify();
	}

	@Override
	public synchronized MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		String key = MessageChannel.getKey(targetHostPort, "TLS");

		if(messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		}

		TLSMessageChannel retval = new TLSMessageChannel(targetHostPort.getInetAddress(), targetHostPort.getPort(),
				sipStack, this);

		this.messageChannels.put(key, retval);

		retval.isCached = true;

		logger.log(Level.FINEST, "key {0}", key);
		logger.log(Level.FINEST, "Creating {0}", retval);

		return retval;
	}

	@Override
	public synchronized MessageChannel createMessageChannel(InetAddress host, int port) throws IOException {
		try {
			String key = MessageChannel.getKey(host, port, "TLS");
			if(messageChannels.get(key) != null) {
				return this.messageChannels.get(key);
			}

			TLSMessageChannel retval = new TLSMessageChannel(host, port, sipStack, this);

			this.messageChannels.put(key, retval);

			retval.isCached = true;

			logger.log(Level.FINEST, "key {0}", key);
			logger.log(Level.FINEST, "Creating {0}", retval);

			return retval;
		} catch(UnknownHostException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	/**
	 * Default target port for TLS
	 */
	@Override
	public int getDefaultTargetPort() {
		return 5061;
	}

	/**
	 * TLS is a secure protocol.
	 */
	@Override
	public boolean isSecure() {
		return true;
	}
}
