package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sit in a loop waiting for incoming tcp connections and start a new thread to
 * handle each new connection. This is the active object that creates new TCP
 * MessageChannels (one for each new accept socket).
 */
public class TCPMessageProcessor extends ConnectionOrientedMessageProcessor implements Runnable {
	private static final Logger logger = Logger.getLogger(TCPMessageProcessor.class.getName());

	/**
	 * Constructor.
	 * 
	 * @param sipStack SIPStack structure.
	 * @param port     port where this message processor listens.
	 */
	protected TCPMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, port, "tcp", sipStack);
	}

	/**
	 * Start the processor.
	 */
	public void start() throws IOException {
		Thread thread = new Thread(this);

		thread.setName("MessageProcessorThread-TCP-" + getIpAddress().getHostAddress() + '/' + getPort());
		thread.setPriority(sipStack.getThreadPriority());
		thread.setDaemon(true);

		this.sock = sipStack.getNetworkLayer().createServerSocket(getPort(), 0, getIpAddress());

		if(getIpAddress().getHostAddress().equals(IN_ADDR_ANY)
				|| getIpAddress().getHostAddress().equals(IN6_ADDR_ANY)) {
			// Store the address to which we are actually bound
			super.setIpAddress(sock.getInetAddress());
		}

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
			try {
				synchronized (this) {
					/*
					 * sipStack.maxConnections == -1 means we are willing to handle an "infinite" number of simultaneous
					 * connections (no resource limitation). This is the default behavior.
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

				Socket newsock = sock.accept();

				if (sipStack.isTcpNoDelayEnabled) {
					newsock.setTcpNoDelay(true);
				}

				logger.log(Level.FINEST, "Accepting new connection!");

				// Note that for an incoming message channel, the thread is already running
				TCPMessageChannel newChannel = new TCPMessageChannel(newsock, sipStack, this, "TCPMessageChannelThread-"
						+ nConnections);

				logger.log(Level.FINEST, "{0} adding incoming channel {1} for processor {2}:{3}/{4}",
						new Object[] { Thread.currentThread(), newChannel.getKey(), getIpAddress(), getPort(),
								getTransport() });

				incomingMessageChannels.put(newChannel.getKey(), newChannel);
			} catch(SocketException ex) {
				this.isRunning = false;
			} catch(IOException ex) {
				// Problem accepting connection.
				logger.log(Level.SEVERE, ex.getMessage(), ex);

				continue;
			} catch(Exception ex) {
				InternalErrorHandler.handleException(ex);
			}
		}
	}

	/**
	 * Return the transport string.
	 * 
	 * @return the transport string
	 */
	@Override
	public String getTransport() {
		return "tcp";
	}

	/**
	 * Stop the message processor.
	 */
	@Override
	public synchronized void stop() {
		isRunning = false;

		try {
			if(sock == null) {
				logger.log(Level.FINEST, "Socket was null, perhaps not started properly");
			} else {
				sock.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collection<ConnectionOrientedMessageChannel> en = messageChannels.values();
		for(Iterator<ConnectionOrientedMessageChannel> it = en.iterator(); it.hasNext();) {
			it.next().close();
		}

		// RRPN: fix
		for(Iterator<ConnectionOrientedMessageChannel> incomingMCIterator = incomingMessageChannels.values().iterator();
				incomingMCIterator.hasNext();) {
			incomingMCIterator.next().close();
		}

		this.notify();
	}

	@Override
	public synchronized MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		String key = MessageChannel.getKey(targetHostPort, "TCP");

		if(messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		}

		TCPMessageChannel retval = new TCPMessageChannel(targetHostPort.getInetAddress(), targetHostPort.getPort(),
				sipStack, this);

		this.messageChannels.put(key, retval);

		retval.isCached = true;

		logger.log(Level.FINEST, "key: {0}", key);
		logger.log(Level.FINEST, "Creating: {0}", retval);

		return retval;
	}

	public synchronized MessageChannel createMessageChannel(InetAddress host, int port) throws IOException {
		try {
			String key = MessageChannel.getKey(host, port, "TCP");

			if(messageChannels.get(key) != null) {
				return this.messageChannels.get(key);
			}

			TCPMessageChannel retval = new TCPMessageChannel(host, port, sipStack, this);

			this.messageChannels.put(key, retval);

			retval.isCached = true;

			logger.log(Level.FINEST, "key: {0}", key);
			logger.log(Level.FINEST, "Creating: {0}", retval);

			return retval;
		} catch (UnknownHostException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	/**
	 * Default target port for TCP
	 */
	@Override
	public int getDefaultTargetPort() {
		return 5060;
	}

	/**
	 * TCP is not a secure protocol.
	 */
	@Override
	public boolean isSecure() {
		return false;
	}
}
