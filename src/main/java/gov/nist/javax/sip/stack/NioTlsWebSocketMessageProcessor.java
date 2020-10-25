package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;

import javax.net.ssl.SSLContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioTlsWebSocketMessageProcessor extends NioWebSocketMessageProcessor {
	private static final Logger logger = Logger.getLogger(NioTlsWebSocketMessageProcessor.class.getName());

	SSLContext sslServerCtx;
	SSLContext sslClientCtx;

	private static final int MAX_WAIT_ATTEMPTS = 100;

	public NioTlsWebSocketMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, sipStack, port);

		// by default it's WSS, can be overridden by TLS accelerator
		transport = "WSS";

		try {
			init();
		} catch(Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel client) throws IOException {
		logger.log(Level.FINEST, "NioTlsWebSocketMessageProcessor::createMessageChannel: {0} client: {1}",
				new Object[] { nioTcpMessageProcessor, client });

		return NioTlsWebSocketMessageChannel.create(NioTlsWebSocketMessageProcessor.this, client);
	}

	@Override
	public synchronized MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		logger.entering(NioTlsWebSocketMessageProcessor.class.getName(), "createMessageChannel", targetHostPort);

		NioTlsWebSocketMessageChannel retval = null;
		try {
			String key = MessageChannel.getKey(targetHostPort, transport);

			if(messageChannels.get(key) != null) {
				retval = (NioTlsWebSocketMessageChannel) this.messageChannels.get(key);
				int wait;

				for(wait = 0; wait <= MAX_WAIT_ATTEMPTS; wait++) {
					if(retval.readingHttp) {
						try {
							logger.log(Level.FINEST, "NioTlsWebSocketMessageProcessor::createMessageChannel: waiting"
									+ " for TLS/HTTP handshake");

							Thread.sleep(100);
						} catch(InterruptedException e) {
							logger.log(Level.FINEST, "silently ignoring exception", e);
						}
					} else {
						logger.log(Level.FINEST, "NioTlsWebSocketMessageProcessor::createMessageChannel:"
								+ " after handshake wait: {0}", wait);

						break;
					}
				}

				if(MAX_WAIT_ATTEMPTS == wait) {
					throw new IOException("Timed out waiting for TLS handshake");
				}

				return retval;
			} else {
				retval = new NioTlsWebSocketMessageChannel(targetHostPort.getInetAddress(), targetHostPort.getPort(),
						sipStack, this);

				synchronized(messageChannels) {
					this.messageChannels.put(key, retval);
				}

				retval.isCached = true;

				logger.log(Level.FINEST, "key: {0}\nCreating: {1}", new Object[] { key, retval });

				selector.wakeup();

				return retval;
			}
		} finally {
			logger.log(Level.FINEST, "MessageChannel::createMessageChannel - exit: {0}", retval);
		}
	}

	@Override
	public synchronized MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
		String key = MessageChannel.getKey(targetHost, port, transport);

		if(messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		}

		NioTlsWebSocketMessageChannel retval = new NioTlsWebSocketMessageChannel(targetHost, port, sipStack, this);

		selector.wakeup();

		this.messageChannels.put(key, retval);

		retval.isCached = true;

		logger.log(Level.FINEST, "key: {0}\nCreating: {1}", new Object[] { key, retval });

		return retval;
	}

	public void init() throws Exception {
		if (sipStack.securityManagerProvider.getKeyManagers(false) == null
				|| sipStack.securityManagerProvider.getTrustManagers(false) == null
				|| sipStack.securityManagerProvider.getTrustManagers(true) == null) {
			logger.log(Level.FINEST, "TLS initialization failed due to NULL security config");

			// The settings
			return;
		}

		sslServerCtx = SSLContext.getInstance("TLS");
		sslClientCtx = SSLContext.getInstance("TLS");

		if (sipStack.getClientAuth() == ClientAuthType.DisabledAll) {
			logger.log(Level.FINEST, "ClientAuth: {0} bypassing all cert validations", sipStack.getClientAuth());

			sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false),
					NioTlsMessageProcessor.trustAllCerts, null);
			sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true),
					NioTlsMessageProcessor.trustAllCerts, null);
		} else {
			logger.log(Level.FINEST, "ClientAuth {0}", sipStack.getClientAuth());

			sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false),
					sipStack.securityManagerProvider.getTrustManagers(false), null);
			sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true),
					sipStack.securityManagerProvider.getTrustManagers(true), null);
		}
	}
}
