package gov.nist.core.net;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.ClientAuthType;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * extended implementation of a network layer that allows to define a private java keystores/truststores
 */
public class SslNetworkLayer implements NetworkLayer {
	private static Logger logger = Logger.getLogger(SslNetworkLayer.class.getName());

	private SSLSocketFactory sslSocketFactory;
	private SSLServerSocketFactory sslServerSocketFactory;

	// Create a trust manager that does not validate certificate chains
	TrustManager[] trustAllCerts = new TrustManager[] { 
		new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
				return new X509Certificate[0]; 
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				if(logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST,  "checkClientTrusted : Not validating certs {0} authType {1}",
							new Object[] {certs, authType});
				}
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				if(logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "checkServerTrusted : Not validating certs {0} authType {1}",
							new Object[] {certs, authType});
				}
			}
		}
	};

	public SslNetworkLayer(SipStackImpl sipStack, String trustStoreFile, String keyStoreFile, char[] keyStorePassword,
			char[] trustStorePassword, String keyStoreType, String trustStoreType)
					throws GeneralSecurityException, IOException {
		SSLContext sslContext;

		sslContext = SSLContext.getInstance("TLS");

		String algorithm = KeyManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(algorithm);
		KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
		SecureRandom secureRandom = new SecureRandom();

		secureRandom.nextInt();

		KeyStore keyStore = KeyStore.getInstance(keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());
		KeyStore trustStore = KeyStore.getInstance(trustStoreType != null ? trustStoreType : KeyStore.getDefaultType());

		keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
		trustStore.load(new FileInputStream(trustStoreFile), trustStorePassword);
		tmFactory.init(trustStore);
		kmFactory.init(keyStore, keyStorePassword);

		if(sipStack.getClientAuth() == ClientAuthType.DisabledAll) {
			if(logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "ClientAuth {0} bypassing all cert validations", sipStack.getClientAuth());
			}

			sslContext.init(null, trustAllCerts, secureRandom);
		} else {
			if(logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "ClientAuth {0}", sipStack.getClientAuth());
			}

			sslContext.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), secureRandom);
		}

		sslServerSocketFactory = sslContext.getServerSocketFactory();
		sslSocketFactory = sslContext.getSocketFactory();
	}

	public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddress) throws IOException {
		return new ServerSocket(port, backlog, bindAddress);
	}

	public Socket createSocket(InetAddress address, int port) throws IOException {
		return new Socket(address, port);
	}

	public DatagramSocket createDatagramSocket() throws SocketException {
		return new DatagramSocket();
	}

	public DatagramSocket createDatagramSocket(int port, InetAddress laddr) throws SocketException {
		return new DatagramSocket(port, laddr);
	}

	public SSLServerSocket createSSLServerSocket(int port, int backlog, InetAddress bindAddress) throws IOException {
		return (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, backlog, bindAddress);
	}

	public SSLSocket createSSLSocket(InetAddress address, int port) throws IOException {
		return createSSLSocket(address, port, null);
	}

	public SSLSocket createSSLSocket(InetAddress address, int port, InetAddress myAddress) throws IOException {
		SSLSocket sock = (SSLSocket) sslSocketFactory.createSocket();

		if(myAddress != null) {
			// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
			// and let the JDK pick an ephemeral port
			sock.bind(new InetSocketAddress(myAddress, 0));
		}

		try {
			sock.connect(new InetSocketAddress(address, port), 8000);
		} catch (SocketTimeoutException e) {
			throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
		}

		return sock;
	}

	public Socket createSocket(InetAddress address, int port, InetAddress myAddress) throws IOException {
		Socket sock = new Socket();

		if(null == myAddress) {
			try {
				sock.connect(new InetSocketAddress(address, port), 8000);
			} catch (SocketTimeoutException e) {
				throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
			}

			return sock;
		}

		// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
		// and let the JDK pick an ephemeral port
		sock.bind(new InetSocketAddress(myAddress, 0));

		try {
			sock.connect(new InetSocketAddress(address, port), 8000);
		} catch (SocketTimeoutException e) {
			throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
		}

		return sock;
	}

	/**
	 * Creates a new Socket, binds it to myAddress:myPort and connects it to
	 * address:port.
	 *
	 * @param address the InetAddress that we'd like to connect to.
	 * @param port the port that we'd like to connect to
	 * @param myAddress the address that we are supposed to bind on or null
	 *        for the "any" address.
	 * @param myPort the port that we are supposed to bind on or 0 for a random
	 * one.
	 *
	 * @return a new Socket, bound on myAddress:myPort and connected to
	 * address:port.
	 * @throws IOException if binding or connecting the socket fail for a reason
	 * (exception relayed from the corresponding Socket methods)
	 */
	public Socket createSocket(InetAddress address, int port, InetAddress myAddress, int myPort) throws IOException {
		if (myAddress != null) {
			Socket sock = new Socket();
			// trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
			// and let the JDK pick an ephemeral port    
			sock.bind(new InetSocketAddress(myAddress, 0));
			try {
				sock.connect(new InetSocketAddress(address, port), 8000);
			} catch (SocketTimeoutException e) {
				throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
			}

			return sock;
		} else {
			Socket sock =  new Socket();
			if(myPort != 0) {
				sock.bind(new InetSocketAddress(port));
			}

			try {
				sock.connect(new InetSocketAddress(address, port), 8000);
			} catch (SocketTimeoutException e) {
				throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
			}

			return sock;
		}
	}

	@Override
	public void setSipStack(SipStackImpl sipStackImpl) {
		logger.entering(SslNetworkLayer.class.getName(), "setSipStack", sipStackImpl);
	}
}
