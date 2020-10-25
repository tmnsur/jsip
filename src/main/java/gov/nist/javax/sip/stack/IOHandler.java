package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.SipStackImpl;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low level Input output to a socket. Caches TCP connections and takes care of
 * re-connecting to the remote party if the other end drops the connection
 */
public class IOHandler {
	private static final Logger logger = Logger.getLogger(IOHandler.class.getName());

	private SipStackImpl sipStack;
	private static final String TCP = "tcp";
	private static final String TLS = "tls";

	private static final String INADDR_PORT = "inaddr = {0} port = {1}";

	// A cache of client sockets that can be re-used for sending TCP messages.
	private final ConcurrentHashMap<String, Socket> socketTable = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Semaphore> socketCreationMap = new ConcurrentHashMap<>();

	protected static String makeKey(InetAddress addr, int port) {
		return addr.getHostAddress() + ":" + port;
	}

	protected static String makeKey(String addr, int port) {
		return addr + ":" + port;
	}

	protected IOHandler(SIPTransactionStack sipStack) {
		this.sipStack = (SipStackImpl) sipStack;
	}

	protected void putSocket(String key, Socket sock) {
		logger.log(Level.FINEST, "adding socket for key {0}", key);

		socketTable.put(key, sock);
	}

	protected Socket getSocket(String key) {
		return socketTable.get(key);

	}

	protected void removeSocket(String key) {
		socketTable.remove(key);

		Semaphore semaphore = socketCreationMap.remove(key);

		if(semaphore != null) {
			semaphore.release();
		}

		logger.log(Level.FINEST, "removed Socket and Semaphore for key {0}", key);
	}

	/**
	 * A private function to write things out. This needs to be synchronized as
	 * writes can occur from multiple threads. We write in chunks to allow the other
	 * side to synchronize for large sized writes.
	 */
	private void writeChunks(OutputStream outputStream, byte[] bytes, int length) throws IOException {
		// Chunk size is 16K - this hack is for large writes over slow connections.
		synchronized(outputStream) {
			int chunksize = 8 * 1024;

			for(int p = 0; p < length; p += chunksize) {
				int chunk = p + chunksize < length ? chunksize : length - p;

				outputStream.write(bytes, p, chunk);
			}
		}

		outputStream.flush();
	}

	/**
	 * Creates and binds, if necessary, a socket connected to the specified
	 * destination address and port and then returns its local address.
	 *
	 * @param dst          the destination address that the socket would need to
	 *                     connect to.
	 * @param dstPort      the port number that the connection would be established
	 *                     with.
	 * @param localAddress the address that we would like to bind on (null for the
	 *                     "any" address).
	 * @param localPort    the port that we'd like our socket to bind to (0 for a
	 *                     random port).
	 *
	 * @return the SocketAddress that this handler would use when connecting to the
	 *         specified destination address and port.
	 *
	 * @throws IOException if we fail binding the socket
	 */
	public SocketAddress getLocalAddressForTcpDst(InetAddress dst, int dstPort, InetAddress localAddress, int localPort)
			throws IOException {
		String key = makeKey(dst, dstPort);

		Socket clientSock = getSocket(key);

		if(clientSock == null) {
			clientSock = sipStack.getNetworkLayer().createSocket(dst, dstPort, localAddress, localPort);

			putSocket(key, clientSock);
		}

		return clientSock.getLocalSocketAddress();
	}

	/**
	 * Creates and binds, if necessary, a socket connected to the specified
	 * destination address and port and then returns its local address.
	 *
	 * @param dst          the destination address that the socket would need to
	 *                     connect to.
	 * @param dstPort      the port number that the connection would be established
	 *                     with.
	 * @param localAddress the address that we would like to bind on (null for the
	 *                     "any" address).
	 *
	 * @param channel      the message channel that will be servicing the socket
	 *
	 * @return the SocketAddress that this handler would use when connecting to the
	 *         specified destination address and port.
	 *
	 * @throws IOException if we fail binding the socket
	 */
	public SocketAddress getLocalAddressForTlsDst(InetAddress dst, int dstPort, InetAddress localAddress,
			TLSMessageChannel channel) throws IOException {
		String key = makeKey(dst, dstPort);
		Socket clientSock = getSocket(key);

		if(clientSock == null) {
			clientSock = sipStack.getNetworkLayer().createSSLSocket(dst, dstPort, localAddress);

			SSLSocket sslsock = (SSLSocket) clientSock;

			logger.log(Level.FINEST, INADDR_PORT, new Object[] {dst, dstPort});

			HandshakeCompletedListenerImpl listner = new HandshakeCompletedListenerImpl(channel, sslsock);

			channel.setHandshakeCompletedListener(listner);
			sslsock.addHandshakeCompletedListener(listner);
			sslsock.setEnabledProtocols(sipStack.getEnabledProtocols());
			sslsock.setEnabledCipherSuites(sipStack.getEnabledCipherSuites());

			listner.startHandshakeWatchdog();
			sslsock.startHandshake();
			channel.setHandshakeCompleted(true);

			logger.log(Level.FINEST, "Handshake passed");

			// allow application to enforce policy by validating the certificate
			try {
				sipStack.getTlsSecurityPolicy().enforceTlsPolicy(channel.getEncapsulatedClientTransaction());
			} catch(SecurityException ex) {
				throw new IOException(ex.getMessage());
			}

			logger.log(Level.FINEST, "TLS Security policy passed");

			putSocket(key, clientSock);
		}

		return clientSock.getLocalSocketAddress();
	}

	/**
	 * Send an array of bytes.
	 *
	 * @param receiverAddress -- inet address
	 * @param contactPort     -- port to connect to.
	 * @param transport       -- tcp or udp.
	 * @param isClient        -- retry to connect if the other end closed connection
	 * @throws IOException -- if there is an IO exception sending message.
	 */

	public Socket sendBytes(InetAddress senderAddress, InetAddress receiverAddress, int contactPort, String transport,
			byte[] bytes, boolean isClient, MessageChannel messageChannel) throws IOException {
		int retryCount = 0;
		int maxRetry = isClient ? 2 : 1;
		// Server uses TCP transport. TCP client sockets are cached
		int length = bytes.length;

		logger.log(Level.FINEST, "sendBytes: {0}, local inAddr: {1}, remote inAddr: {2}, port: {3}, length: {4},"
				+ " isClient: {5}", new Object[] {transport, senderAddress.getHostAddress(),
						receiverAddress.getHostAddress(), contactPort, length, isClient});

		if(transport.compareToIgnoreCase(TCP) == 0) {
			String key = makeKey(receiverAddress, contactPort);

			Socket clientSock = null;
			enterIOCriticalSection(key);

			try {
				clientSock = getSocket(key);
				while(retryCount < maxRetry) {
					if(clientSock == null) {
						logger.log(Level.FINEST, INADDR_PORT, new Object[] {receiverAddress, contactPort});
						/*
						 * note that the IP Address for stack may not be assigned. sender address is the address of
						 * the listening point.in version 1.1 all listening points have the same IPaddress (i.e. that
						 * of the stack). In version 1.2 the IP address is on a per listening point basis.
						 */
						try {
							clientSock = sipStack.getNetworkLayer().createSocket(receiverAddress, contactPort,
									senderAddress);
						} catch(SocketException e) {
							// We must catch the socket timeout exceptions here, any SocketException not just ConnectException
							logger.log(Level.SEVERE, "Problem connecting {0} {1} {2} for message {3}", new Object[] {
									receiverAddress, contactPort, senderAddress,
											new String(bytes, StandardCharsets.UTF_8)});

							// new connection is bad. remove from our table the socket and its semaphore

							removeSocket(key);

							throw new SocketException(e.getClass() + " " + e.getMessage() + " " + e.getCause()
									+ " Problem connecting " + receiverAddress + " " + contactPort + " " + senderAddress
									+ " for message " + new String(bytes, StandardCharsets.UTF_8));
						}

						logger.log(Level.FINEST, "local inaddr = {0}", clientSock.getLocalAddress().getHostAddress());

						OutputStream outputStream = clientSock.getOutputStream();

						writeChunks(outputStream, bytes, length);
						putSocket(key, clientSock);

						break;
					} else {
						try {
							OutputStream outputStream = clientSock.getOutputStream();
							writeChunks(outputStream, bytes, length);
							break;
						} catch (IOException ex) {
							logger.log(Level.WARNING, "IOException occured retryCount {0}", retryCount);

							try {
								clientSock.close();
							} catch(Exception e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
							}

							clientSock = null;

							retryCount++;

							// This is a server TX trying to send a response.
							if(!isClient) {
								removeSocket(key);

								throw ex;
							}

							if(retryCount >= maxRetry) {
								// old connection is bad. remove from our table the socket and its semaphore
								removeSocket(key);
							} else {
								// don't remove the semaphore on retry
								socketTable.remove(key);
							}
						}
					}
				}
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Problem sending: sendBytes: {0} inAddr: {1} port: {2} remoteHost: {3}"
						+ " remotePort: {4} peerPacketPort: {5} isClient: {6}", new Object[] {transport,
								receiverAddress.getHostAddress(), contactPort, messageChannel.getPeerAddress(),
										messageChannel.getPeerPort(), messageChannel.getPeerPacketSourcePort(),
												isClient});

				removeSocket(key);
			} finally {
				leaveIOCriticalSection(key);
			}

			if(clientSock == null) {
				logger.log(Level.SEVERE, "{0} could not connect to {1}:{2}", new Object[] {this.socketTable,
						receiverAddress, contactPort});

				throw new IOException("Could not connect to " + receiverAddress + ":" + contactPort);
			}

			return clientSock;
		} else if(transport.compareToIgnoreCase(TLS) == 0) {
			String key = makeKey(receiverAddress, contactPort);
			Socket clientSock = null;

			enterIOCriticalSection(key);

			try {
				clientSock = getSocket(key);

				while(retryCount < maxRetry) {
					if(clientSock == null) {
						clientSock = sipStack.getNetworkLayer()
								.createSSLSocket(receiverAddress, contactPort, senderAddress);

						SSLSocket sslsock = (SSLSocket) clientSock;

						logger.log(Level.FINEST, INADDR_PORT, new Object[] {receiverAddress, contactPort});

						HandshakeCompletedListenerImpl listner = new HandshakeCompletedListenerImpl(
								(TLSMessageChannel) messageChannel, clientSock);
						((TLSMessageChannel) messageChannel).setHandshakeCompletedListener(listner);
						sslsock.addHandshakeCompletedListener(listner);
						sslsock.setEnabledProtocols(sipStack.getEnabledProtocols());

						listner.startHandshakeWatchdog();
						sslsock.startHandshake();
						((TLSMessageChannel) messageChannel).setHandshakeCompleted(true);

						logger.log(Level.FINEST, "Handshake passed");

						// allow application to enforce policy by validating the certificate

						try {
							sipStack.getTlsSecurityPolicy()
									.enforceTlsPolicy(messageChannel.getEncapsulatedClientTransaction());
						} catch (SecurityException ex) {
							throw new IOException(ex.getMessage());
						}

						logger.log(Level.FINEST, "TLS Security policy passed");

						OutputStream outputStream = clientSock.getOutputStream();
						writeChunks(outputStream, bytes, length);
						putSocket(key, clientSock);
						break;
					} else {
						try {
							OutputStream outputStream = clientSock.getOutputStream();
							writeChunks(outputStream, bytes, length);
							break;
						} catch (IOException ex) {
							logger.log(Level.SEVERE, ex.getMessage(), ex);

							// old connection is bad. remove from our table.
							removeSocket(key);

							try {
								logger.log(Level.FINEST, "Closing socket");

								clientSock.close();
							} catch(Exception e) {
								logger.log(Level.FINEST, "silently ignoring exception", e);
							}

							clientSock = null;

							retryCount++;
						}
					}
				}
			} catch(SSLHandshakeException ex) {
				removeSocket(key);

				throw ex;
			} catch (IOException ex) {
				removeSocket(key);
			} finally {
				leaveIOCriticalSection(key);
			}

			if(clientSock == null) {
				throw new IOException("Could not connect to " + receiverAddress + ":" + contactPort);
			}

			return clientSock;
		}

		// This is a UDP transport...
		DatagramSocket datagramSock = sipStack.getNetworkLayer().createDatagramSocket();
		datagramSock.connect(receiverAddress, contactPort);
		DatagramPacket dgPacket = new DatagramPacket(bytes, 0, length, receiverAddress, contactPort);
		datagramSock.send(dgPacket);
		datagramSock.close();

		return null;
	}

	private void leaveIOCriticalSection(String key) {
		Semaphore creationSemaphore = socketCreationMap.get(key);
		if (creationSemaphore != null) {
			creationSemaphore.release();
		}
	}

	private void enterIOCriticalSection(String key) throws IOException {
		Semaphore creationSemaphore = socketCreationMap.get(key);

		if (creationSemaphore == null) {
			Semaphore newCreationSemaphore = new Semaphore(1, true);
			creationSemaphore = socketCreationMap.putIfAbsent(key, newCreationSemaphore);
			if (creationSemaphore == null) {
				creationSemaphore = newCreationSemaphore;

				logger.log(Level.FINEST, "new Semaphore added for key {0}", key);
			}
		}

		try {
			boolean retval = creationSemaphore.tryAcquire(10, TimeUnit.SECONDS);
			if (!retval) {
				throw new IOException("Could not acquire IO Semaphore'" + key + "' after 10 seconds -- giving up ");
			}
		} catch (InterruptedException e) {
			throw new IOException("exception in acquiring sem");
		}
	}

	/**
	 * Close all the cached connections.
	 */
	public void closeAll() {
		logger.log(Level.FINEST, "Closing {0} sockets from IOHandler", socketTable.size());

		for(Enumeration<Socket> values = socketTable.elements(); values.hasMoreElements();) {
			Socket s = values.nextElement();

			try {
				s.close();
			} catch(IOException ex) {
				logger.log(Level.FINEST, "silently ignoring exception", ex);
			}
		}
	}
}
