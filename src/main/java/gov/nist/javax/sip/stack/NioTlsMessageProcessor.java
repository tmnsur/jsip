package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioTlsMessageProcessor extends NioTcpMessageProcessor {
	private static final Logger logger = Logger.getLogger(NioTlsMessageProcessor.class.getName());

	// Create a trust manager that does not validate certificate chains
	protected static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			logger.log(Level.FINEST, "checkClientTrusted: Not validating certs: {0}, authType: {1}",
					new Object[] { certs, authType });
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			logger.log(Level.FINEST, "checkServerTrusted: Not validating certs: {0}, authType: {1}",
					new Object[] { certs, authType });
		}
	}};

	SSLContext sslServerCtx;
	SSLContext sslClientCtx;

	public NioTlsMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, sipStack, port);

		transport = "TLS";

		try {
			init();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel client) throws IOException {
		return NioTlsMessageChannel.create(NioTlsMessageProcessor.this, client);
	}

	@Override
	public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		logger.entering(NioTlsMessageProcessor.class.getName(), "createMessageChannel", targetHostPort);

		NioTlsMessageChannel retval = null;
		try {
			String key = MessageChannel.getKey(targetHostPort, "TLS");

			if (messageChannels.get(key) != null) {
				retval = (NioTlsMessageChannel) this.messageChannels.get(key);

				return retval;
			}

			retval = new NioTlsMessageChannel(targetHostPort.getInetAddress(), targetHostPort.getPort(), sipStack,
					this);

			synchronized(messageChannels) {
				this.messageChannels.put(key, retval);
			}

			retval.isCached = true;

			logger.log(Level.FINEST, "key: {0}\nCreating: {1}", new Object[] { key, retval });

			selector.wakeup();

			return retval;
		} finally {
			logger.log(Level.FINEST, "MessageChannel::createMessageChannel - exit: {0}", retval);
		}
	}

	@Override
	public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
		String key = MessageChannel.getKey(targetHost, port, "TLS");
		if (messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		} else {
			NioTlsMessageChannel retval = new NioTlsMessageChannel(targetHost, port, sipStack, this);

			selector.wakeup();

			this.messageChannels.put(key, retval);

			retval.isCached = true;

			logger.log(Level.FINEST, "key {0}\nCreating: {1}", new Object[] {key, retval});

			return retval;
		}
	}

	public void init() throws Exception {
		if(null == sipStack.securityManagerProvider.getKeyManagers(false)
				|| null == sipStack.securityManagerProvider.getTrustManagers(false)
				|| null == sipStack.securityManagerProvider.getTrustManagers(true)) {
			logger.log(Level.FINEST, "TLS initialization failed due to NULL security config");

			// The settings
			return;
		}

		sslServerCtx = SSLContext.getInstance("TLS");
		sslClientCtx = SSLContext.getInstance("TLS");

		if(sipStack.getClientAuth() == ClientAuthType.DisabledAll) {
			logger.log(Level.FINEST, "ClientAuth {0} bypassing all cert validations", sipStack.getClientAuth());

			sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false), trustAllCerts, null);
			sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true), trustAllCerts, null);
		} else {
			logger.log(Level.FINEST, "ClientAuth {0}", sipStack.getClientAuth());

			sslServerCtx.init(sipStack.securityManagerProvider.getKeyManagers(false),
					sipStack.securityManagerProvider.getTrustManagers(false), null);
			sslClientCtx.init(sipStack.securityManagerProvider.getKeyManagers(true),
					sipStack.securityManagerProvider.getTrustManagers(true), null);
		}
	}
}
