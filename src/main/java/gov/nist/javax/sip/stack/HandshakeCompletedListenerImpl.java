package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

public class HandshakeCompletedListenerImpl implements HandshakeCompletedListener {
	private static final Logger logger = Logger.getLogger(HandshakeCompletedListenerImpl.class.getName());

	private HandshakeCompletedEvent handshakeCompletedEvent;
	private final Object eventWaitObject = new Object();

	private HandshakeWatchdog watchdog;
	private SIPTransactionStack sipStack;

	// Added for https://java.net/jira/browse/JSIP-483, NIO doesn't provide a HandshakeCompletedEvent
	private Certificate[] peerCertificates;
	private Certificate[] localCertificates;
	private String cipherSuite;

	public HandshakeCompletedListenerImpl(TLSMessageChannel tlsMessageChannel, Socket socket) {
		tlsMessageChannel.setHandshakeCompletedListener(this);
		sipStack = tlsMessageChannel.getSIPStack();
		if (sipStack.getSslHandshakeTimeout() > 0) {
			this.watchdog = new HandshakeWatchdog(socket);
		}
	}

	public HandshakeCompletedListenerImpl(NioTlsMessageChannel tlsMessageChannel, SocketChannel socket) {
		tlsMessageChannel.setHandshakeCompletedListener(this);
		sipStack = tlsMessageChannel.getSIPStack();
		if (sipStack.getSslHandshakeTimeout() > 0) {
			this.watchdog = new HandshakeWatchdog(socket.socket());
		}
	}

	public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
		if (this.watchdog != null) {
			sipStack.getTimer().cancel(watchdog);
			this.watchdog = null;
		}
		this.handshakeCompletedEvent = handshakeCompletedEvent;
		synchronized (eventWaitObject) {
			eventWaitObject.notify();
		}
	}

	/**
	 * Gets the event indicating that the SSL handshake has completed. The method
	 * waits until the event has been obtained by the listener or a timeout of 5
	 * seconds has elapsed.
	 * 
	 * @return the handshakeCompletedEvent or null when the timeout elapsed
	 */
	public HandshakeCompletedEvent getHandshakeCompletedEvent() {
		try {
			synchronized (eventWaitObject) {
				if (handshakeCompletedEvent == null)
					eventWaitObject.wait(5000);
			}
		} catch(InterruptedException e) {
			logger.log(Level.FINEST, "silently ignoring exception", e);

			// we don't care
		}
		return handshakeCompletedEvent;
	}

	public void startHandshakeWatchdog() {
		if (this.watchdog != null) {
			logger.log(Level.INFO, "starting watchdog for socket {0} on sslhandshake {1}", new Object[] {
					watchdog.socket, sipStack.getSslHandshakeTimeout()});

			sipStack.getTimer().schedule(watchdog, sipStack.getSslHandshakeTimeout());
		}
	}

	/**
	 * @return the peerCertificates
	 */
	public Certificate[] getPeerCertificates() {
		return peerCertificates;
	}

	/**
	 * @param peerCertificates the peerCertificates to set
	 */
	public void setPeerCertificates(Certificate[] peerCertificates) {
		this.peerCertificates = peerCertificates;
	}

	/**
	 * @return the cipherSuite
	 */
	public String getCipherSuite() {
		return cipherSuite;
	}

	/**
	 * @param cipherSuite the cipherSuite to set
	 */
	public void setCipherSuite(String cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	/**
	 * @return the localCertificates
	 */
	public Certificate[] getLocalCertificates() {
		return localCertificates;
	}

	/**
	 * @param localCertificates the localCertificates to set
	 */
	public void setLocalCertificates(Certificate[] localCertificates) {
		this.localCertificates = localCertificates;
	}

	class HandshakeWatchdog extends SIPStackTimerTask {
		Socket socket;

		private HandshakeWatchdog(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void runTask() {
			logger.log(Level.INFO, "closing socket {0} on sslhandshaketimeout", socket);

			try {
				socket.close();
			} catch(IOException ex) {
				logger.log(Level.INFO, "couldn''t close socket on sslhandshaketimeout");
			}

			logger.log(Level.INFO, "socket closed {0} on sslhandshaketimeout", socket);
		}
	}
}
