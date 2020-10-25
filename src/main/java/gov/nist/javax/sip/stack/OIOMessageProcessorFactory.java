package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.net.InetAddress;

import javax.sip.ListeningPoint;

/**
 * Default stack implementation of the MessageProcessorFactory.
 * This Factory creates MessageProcessor instances using the Old IO (as opposed to NIO)
 */
public class OIOMessageProcessorFactory implements MessageProcessorFactory {
	@Override
	public MessageProcessor createMessageProcessor(SIPTransactionStack sipStack, InetAddress ipAddress, int port,
			String transport) throws IOException {
		if(transport.equalsIgnoreCase(ListeningPoint.UDP)) {
			UDPMessageProcessor udpMessageProcessor = new UDPMessageProcessor(ipAddress, sipStack, port);
			sipStack.udpFlag = true;

			return udpMessageProcessor;
		}

		if(transport.equalsIgnoreCase(ListeningPoint.TCP)) {
			return new TCPMessageProcessor(ipAddress, sipStack, port);
		}

		if(transport.equalsIgnoreCase(ListeningPoint.TLS)) {
			return new TLSMessageProcessor(ipAddress, sipStack, port);
		}

		if(transport.equalsIgnoreCase(ListeningPoint.SCTP)) {
			/* 
			 * Need Java 7 for this, so these classes are packaged in a separate jars. Try to load it indirectly,
			 * if fails report an error
			 */
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
		} else {
			throw new IllegalArgumentException("bad transport");
		}
	}
}
