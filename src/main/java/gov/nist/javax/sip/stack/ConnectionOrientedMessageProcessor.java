package gov.nist.javax.sip.stack;

import gov.nist.core.Host;
import gov.nist.core.HostPort;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ConnectionOrientedMessageProcessor extends MessageProcessor {
	private static final Logger logger = Logger.getLogger(ConnectionOrientedMessageProcessor.class.getName());

	protected int nConnections;
	protected boolean isRunning;
	protected final Map<String, ConnectionOrientedMessageChannel> messageChannels;
	protected final Map<String, ConnectionOrientedMessageChannel> incomingMessageChannels;
	protected ServerSocket sock;
	protected int useCount;

	public ConnectionOrientedMessageProcessor(InetAddress ipAddress, int port, String transport,
			SIPTransactionStack sipStack) {
		super(ipAddress, port, transport, sipStack);

		this.sipStack = sipStack;

		this.messageChannels = new ConcurrentHashMap<>();
		this.incomingMessageChannels = new ConcurrentHashMap<>();
	}

	/**
	 * Returns the stack.
	 * 
	 * @return my sip stack.
	 */
	public SIPTransactionStack getSIPStack() {
		return sipStack;
	}

	protected synchronized void remove(ConnectionOrientedMessageChannel messageChannel) {
		String key = messageChannel.getKey();

		logger.log(Level.FINEST, "{0} removing {1} for processor {2}:{3}/{4}",
				new Object[] {Thread.currentThread(), key, getIpAddress(), getPort(), getTransport()});

		/** May have been removed already */
		if(messageChannels.get(key) == messageChannel) {
			this.messageChannels.remove(key);
		}

		logger.log(Level.FINEST, "{0} Removing incoming channel {1} for processor {2}:{3}/{4}",
				new Object[] { Thread.currentThread(), key, getIpAddress(), getPort(), getTransport() });

		incomingMessageChannels.remove(key);
	}

	protected synchronized void cacheMessageChannel(ConnectionOrientedMessageChannel messageChannel) {
		String key = messageChannel.getKey();

		ConnectionOrientedMessageChannel currentChannel = messageChannels.get(key);

		if(currentChannel != null) {
			logger.log(Level.FINEST, "Closing {0}", key);

			currentChannel.close();
		}

		logger.log(Level.FINEST, "Caching {0}", key);

		this.messageChannels.put(key, messageChannel);
	}

	/**
	 * TCP can handle an unlimited number of bytes.
	 */
	@Override
	public int getMaximumMessageSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean inUse() {
		return this.useCount != 0;
	}

	public boolean closeReliableConnection(String peerAddress, int peerPort) {
		validatePortInRange(peerPort);

		HostPort hostPort = new HostPort();
		hostPort.setHost(new Host(peerAddress));
		hostPort.setPort(peerPort);

		String messageChannelKey = MessageChannel.getKey(hostPort, "TCP");

		synchronized (this) {
			ConnectionOrientedMessageChannel foundMessageChannel = messageChannels.get(messageChannelKey);

			if(foundMessageChannel != null) {
				foundMessageChannel.close();

				logger.log(Level.FINEST, "{0} Removing channel {1} for processor {2}:{3}/{4}", new Object[] {
						Thread.currentThread(), messageChannelKey, getIpAddress(), getPort(), getTransport()});

				incomingMessageChannels.remove(messageChannelKey);
				messageChannels.remove(messageChannelKey);

				return true;
			}

			foundMessageChannel = incomingMessageChannels.get(messageChannelKey);

			if(foundMessageChannel != null) {
				foundMessageChannel.close();

				logger.log(Level.FINEST, "{0} Removing incoming channel {1} for processor {2}:{3}/{4}", new Object[] {
						Thread.currentThread(), messageChannelKey, getIpAddress(), getPort(), getTransport()});

				incomingMessageChannels.remove(messageChannelKey);
				messageChannels.remove(messageChannelKey);

				return true;
			}
		}

		return false;
	}

	public boolean setKeepAliveTimeout(String peerAddress, int peerPort, long keepAliveTimeout) {
		validatePortInRange(peerPort);

		HostPort hostPort = new HostPort();

		hostPort.setHost(new Host(peerAddress));
		hostPort.setPort(peerPort);

		String messageChannelKey = MessageChannel.getKey(hostPort, "TCP");

		ConnectionOrientedMessageChannel foundMessageChannel = messageChannels.get(messageChannelKey);

		logger.log(Level.FINEST, "{0} checking channel with key {1} : {2} for processor {3}:{4}/{5}", new Object[] {
				Thread.currentThread(), messageChannelKey, foundMessageChannel, getIpAddress(), getPort(),
						getTransport()});

		if(foundMessageChannel != null) {
			foundMessageChannel.setKeepAliveTimeout(keepAliveTimeout);

			return true;
		}

		foundMessageChannel = incomingMessageChannels.get(messageChannelKey);

		logger.log(Level.FINEST, "{0} checking incoming channel with key {1} : {2} for processor {3}:{4}/{5}",
				new Object[] {Thread.currentThread(), messageChannelKey, foundMessageChannel, getIpAddress(),
						getPort(), getTransport()});

		if(foundMessageChannel != null) {
			foundMessageChannel.setKeepAliveTimeout(keepAliveTimeout);

			return true;
		}

		return false;
	}

	protected void validatePortInRange(int port) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("Peer port should be greater than 0 and less 65535, port = " + port);
		}
	}
}
