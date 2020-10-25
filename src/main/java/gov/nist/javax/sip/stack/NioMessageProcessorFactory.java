package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.ListeningPointExt;
import gov.nist.javax.sip.SipStackImpl;

import java.io.IOException;
import java.net.InetAddress;

import javax.sip.ListeningPoint;

public class NioMessageProcessorFactory implements MessageProcessorFactory {
	@Override
	public MessageProcessor createMessageProcessor(SIPTransactionStack sipStack, InetAddress ipAddress, int port,
			String transport) throws IOException {
		if (transport.equalsIgnoreCase(ListeningPoint.UDP)) {
			UDPMessageProcessor udpMessageProcessor = new UDPMessageProcessor(ipAddress, sipStack, port);
			sipStack.udpFlag = true;
			return udpMessageProcessor;
		} else if (transport.equalsIgnoreCase(ListeningPoint.TCP)) {
			NioTcpMessageProcessor nioTcpMessageProcessor = new NioTcpMessageProcessor(ipAddress, sipStack, port);

			return nioTcpMessageProcessor;
		} else if (transport.equalsIgnoreCase(ListeningPoint.TLS)) {
			NioTlsMessageProcessor tlsMessageProcessor = new NioTlsMessageProcessor(ipAddress, sipStack, port);

			return tlsMessageProcessor;
		} else if(transport.equalsIgnoreCase(ListeningPoint.SCTP)) {
			// Need Java 7 for this, so these classes are packaged in a separate
			// jar
			// Try to load it indirectly, if fails report an error
			try {
				Class<?> mpc = ClassLoader.getSystemClassLoader()
						.loadClass("gov.nist.javax.sip.stack.sctp.SCTPMessageProcessor");

				MessageProcessor mp = (MessageProcessor) mpc.newInstance();

				mp.initialize(ipAddress, port, sipStack);

				return mp;
			} catch(ClassNotFoundException e) {
				throw new IllegalArgumentException("SCTP not supported (needs Java 7 and SCTP jar in classpath)");
			} catch(InstantiationException | IllegalAccessException ie) {
				throw new IllegalArgumentException("Error initializing SCTP", ie);
			}
		} else if(transport.equalsIgnoreCase(ListeningPointExt.WS)) {
			if("true".equals(((SipStackImpl) sipStack).getConfigurationProperties()
					.getProperty("gov.nist.javax.sip.USE_TLS_GATEWAY"))) {
				MessageProcessor mp = new NioTlsWebSocketMessageProcessor(ipAddress, sipStack, port);
				mp.transport = "WS";
				return mp;
			} else {
				MessageProcessor mp = new NioWebSocketMessageProcessor(ipAddress, sipStack, port);
				mp.transport = "WS";
				return mp;
			}
		} else if (transport.equalsIgnoreCase("WSS")) {
			if ("true".equals(((SipStackImpl) sipStack).getConfigurationProperties()
					.getProperty("gov.nist.javax.sip.USE_TLS_GATEWAY"))) {
				MessageProcessor mp = new NioWebSocketMessageProcessor(ipAddress, sipStack, port);
				mp.transport = "WSS";
				return mp;
			} else {
				MessageProcessor mp = new NioTlsWebSocketMessageProcessor(ipAddress, sipStack, port);
				mp.transport = "WSS";
				return mp;
			}
		} else {
			throw new IllegalArgumentException("bad transport");
		}
	}
}
