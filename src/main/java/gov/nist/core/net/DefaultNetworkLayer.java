package gov.nist.core.net;

import gov.nist.javax.sip.SipStackImpl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * default implementation which passes straight through to java platform
 */
public class DefaultNetworkLayer implements NetworkLayer {
	private static final Logger logger = Logger.getLogger(DefaultNetworkLayer.class.getName());

	private SSLSocketFactory sslSocketFactory;
	private SSLServerSocketFactory sslServerSocketFactory;

	/**
	 * single default network layer; for flexibility, it may be better not to
	 * make it a singleton, but singleton seems to make sense currently.
	 */
	public static final DefaultNetworkLayer SINGLETON = new DefaultNetworkLayer();

	private DefaultNetworkLayer() {
		sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
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
		if(laddr.isMulticastAddress()) {
			try {
				MulticastSocket ds = new MulticastSocket(port);

				ds.joinGroup(laddr);

				return ds;
			} catch(IOException e) {
				throw new SocketException(e.getLocalizedMessage());
			}
		}

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
			// trying to bind to the correct IP address (in case of multiple VIP addresses by example)
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
		if(myAddress != null) {
			Socket sock = new Socket();
			// http://java.net/jira/browse/JSIP-440 trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
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
			try {
				sock.connect(new InetSocketAddress(address, port), 8000);
			} catch (SocketTimeoutException e) {
				throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
			}

			return sock;
		}
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
		if(myAddress != null) {
			Socket sock = new Socket();
			// http://java.net/jira/browse/JSIP-440 trying to bind to the correct ipaddress (in case of multiple vip addresses by example)
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
			} catch(SocketTimeoutException e) {
				throw new ConnectException("Socket timeout error (8sec)" + address + ":" + port);
			}

			return sock;
		}
	}

	@Override
	public void setSipStack(SipStackImpl sipStackImpl) {
		logger.entering(DefaultNetworkLayer.class.getName(), "setSipStack", sipStackImpl);
	}
}
