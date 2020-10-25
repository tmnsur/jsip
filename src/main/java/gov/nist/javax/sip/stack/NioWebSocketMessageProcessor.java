package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioWebSocketMessageProcessor extends NioTcpMessageProcessor {
	private static final Logger logger = Logger.getLogger(NioWebSocketMessageProcessor.class.getName());

	public NioWebSocketMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, sipStack, port);

		// by default its WS, can be overridden if there is TLS accelerator
		transport = "WS";
	}

	@Override
	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel client) throws IOException {
		return NioWebSocketMessageChannel.create(this, client);
	}

	@Override
	public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		logger.entering(NioWebSocketMessageProcessor.class.getName(), "createMessageChannel", targetHostPort);

		String key = MessageChannel.getKey(targetHostPort, transport);
		if(messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		}

		NioWebSocketMessageChannel retval = new NioWebSocketMessageChannel(targetHostPort.getInetAddress(),
				targetHostPort.getPort(), sipStack, this);

		synchronized(messageChannels) {
			this.messageChannels.put(key, retval);
		}

		retval.isCached = true;

		logger.log(Level.FINEST, "key: {0}\nCreating: {1}", new Object[] { key, retval });

		selector.wakeup();

		logger.exiting(NioWebSocketMessageProcessor.class.getName(), "createMessageChannel", retval);

		return retval;
	}

	@Override
	public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
		String key = MessageChannel.getKey(targetHost, port, transport);
		if (messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		} else {
			NioWebSocketMessageChannel retval = new NioWebSocketMessageChannel(targetHost, port, sipStack, this);

			selector.wakeup();

			this.messageChannels.put(key, retval);

			retval.isCached = true;

			logger.log(Level.FINEST, "Key: {0}\nCreating: {1}", new Object[] { key, retval });

			return retval;
		}
	}
}
