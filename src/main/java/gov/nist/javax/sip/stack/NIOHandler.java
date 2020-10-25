package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low level Input output to a socket. Caches TCP connections and takes care of
 * re-connecting to the remote party if the other end drops the connection
 */
public class NIOHandler {
	private static final Logger logger = Logger.getLogger(NIOHandler.class.getName());

	private NioTcpMessageProcessor messageProcessor;
	// A cache of client sockets that can be re-used for sending tcp messages.
	private final ConcurrentHashMap<String, SocketChannel> socketTable = new ConcurrentHashMap<>();

	KeyedSemaphore keyedSemaphore = new KeyedSemaphore();

	protected static String makeKey(InetAddress addr, int port) {
		return addr.getHostAddress() + ":" + port;
	}

	protected static String makeKey(String addr, int port) {
		return addr + ":" + port;
	}

	protected NIOHandler(SIPTransactionStack sipStack, NioTcpMessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	protected void putSocket(String key, SocketChannel sock) {
		synchronized (socketTable) {
			logger.log(Level.FINEST, "adding socket for key {0}", key);

			socketTable.put(key, sock);
		}
	}

	protected SocketChannel getSocket(String key) {
		// no need to synchronize here
		return socketTable.get(key);
	}

	protected void removeSocket(String key) {
		synchronized(socketTable) {
			socketTable.remove(key);
			keyedSemaphore.remove(key);

			logger.log(Level.FINEST, "removed Socket and Semaphore for key: {0}", key);
		}
	}

	protected void removeSocket(SocketChannel channel) {
		logger.log(Level.FINEST, "Trying to remove cached socketChannel without key: {0} socketChannel: {1}",
				new Object[] {this, channel});

		LinkedList<String> keys = new LinkedList<>();
		synchronized (socketTable) {
			Set<Entry<String, SocketChannel>> e = socketTable.entrySet();
			for (Entry<String, SocketChannel> entry : e) {
				SocketChannel sc = entry.getValue();
				if (sc.equals(channel)) {
					keys.add(entry.getKey());
				}
			}

			for(String key : keys) {
				logger.log(Level.FINEST, "Removing cached socketChannel without key: {0} socketChannel: {1} key: {2}",
						new Object[] {this, channel, key});

				removeSocket(key);
			}
		}
	}

	/**
	 * A private function to write things out. This needs to be synchronized as
	 * writes can occur from multiple threads. We write in chunks to allow the other
	 * side to synchronize for large sized writes.
	 */
	private void writeChunks(SocketChannel channel, byte[] bytes, int length) {
		// Chunk size is 16K - this hack is for large
		// writes over slow connections.
		synchronized (channel) {
			// outputStream.write(bytes,0,length);
			byte[] buff = new byte[length];
			System.arraycopy(bytes, 0, buff, 0, length);
			messageProcessor.send(channel, bytes);
		}
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
	public SocketChannel sendBytes(InetAddress senderAddress, InetAddress receiverAddress, int contactPort,
			String transport, byte[] bytes, boolean isClient, NioTcpMessageChannel messageChannel) throws IOException {
		int retryCount = 0;
		int maxRetry = isClient ? 2 : 1;
		// Server uses TCP transport. TCP client sockets are cached
		int length = bytes.length;

		logger.log(Level.FINEST, "sendBytes: {0} inAddr: {1}, port: {2}, length: {3}, isClient: {4}",
				new Object[] {transport, receiverAddress.getHostAddress(), contactPort, length, isClient});

		String key = makeKey(receiverAddress, contactPort);

		SocketChannel clientSock = null;
		keyedSemaphore.enterIOCriticalSection(key);

		boolean newSocket = false;
		try {
			clientSock = getSocket(key);
			while(retryCount < maxRetry) {
				if(clientSock != null && (!clientSock.isConnected() || !clientSock.isOpen())) {
					removeSocket(key);

					clientSock = null;
					newSocket = true;
				}

				if(clientSock == null) {
					logger.log(Level.FINEST, "inaddr = {0} port = {1}", new Object[] {receiverAddress, contactPort});

					/*
					 * note that the IP Address for stack may not be assigned. sender address is the address of the
					 * listening point. in version 1.1 all listening points have the same IP address (i.e. that of
					 * the stack). In version 1.2 the IP address is on a per listening point basis.
					 */
					try {
						clientSock = messageProcessor
								.blockingConnect(new InetSocketAddress(receiverAddress, contactPort), 10000);
						if (messageChannel instanceof NioTlsMessageChannel) {
							// Added for https://java.net/jira/browse/JSIP-483
							HandshakeCompletedListenerImpl listner = new HandshakeCompletedListenerImpl(
									(NioTlsMessageChannel) messageChannel, clientSock);
							((NioTlsMessageChannel) messageChannel).setHandshakeCompletedListener(listner);
						}
						newSocket = true;
					} catch(SocketException e) {
						// We must catch the socket timeout exceptions here, any SocketException not just ConnectException
						logger.log(Level.SEVERE, "Problem connecting {0} {1} {2} for message {3}",
								new Object[] {receiverAddress, contactPort, senderAddress,
										(messageChannel.isSecure() ? "<<<ENCRYPTED MESSAGE>>>"
												: new String(bytes, StandardCharsets.UTF_8))});

						// new connection is bad. remove from our table the socket and its semaphore
						removeSocket(key);

						throw new SocketException(e.getClass() + " " + e.getMessage() + " " + e.getCause()
								+ " Problem connecting " + receiverAddress + " " + contactPort + " " + senderAddress
								+ " for message " + new String(bytes, StandardCharsets.UTF_8));
					}

					putSocket(key, clientSock);

					break;
				} else {
					break;
				}
			}
		} catch(IOException ex) {
			logger.log(Level.SEVERE, "Problem sending: sendBytes: {0} inAddr: {1} port: {2} remoteHost: {3}"
					+ " remotePort: {4} peerPacketPort: {5} isClient: {6}",
					new Object[] {transport, receiverAddress.getHostAddress(), contactPort,
							messageChannel.getPeerAddress(), messageChannel.getPeerPort(),
									messageChannel.getPeerPacketSourcePort(), isClient});

			removeSocket(key);

			/*
			 * For TCP responses, the transmission of responses is controlled by RFC 3261,
			 * section 18.2.2 :
			 *
			 * o If the "sent-protocol" is a reliable transport protocol such as TCP or
			 * SCTP, or TLS over those, the response MUST be sent using the existing
			 * connection to the source of the original request that created the
			 * transaction, if that connection is still open. This requires the server
			 * transport to maintain an association between server transactions and
			 * transport connections. If that connection is no longer open, the server
			 * SHOULD open a connection to the IP address in the "received" parameter, if
			 * present, using the port in the "sent-by" value, or the default port for that
			 * transport, if no port is specified. If that connection attempt fails, the
			 * server SHOULD use the procedures in [4] for servers in order to determine the
			 * IP address and port to open the connection and send the response to.
			 */
			if(!isClient) {
				receiverAddress = InetAddress.getByName(messageChannel.peerAddressAdvertisedInHeaders);
				contactPort = messageChannel.peerPortAdvertisedInHeaders;

				if(contactPort <= 0) {
					contactPort = 5060;
				}

				key = makeKey(receiverAddress, contactPort);
				clientSock = this.getSocket(key);
				if(clientSock == null || !clientSock.isConnected() || !clientSock.isOpen()) {
					logger.log(Level.FINEST, "inaddr = {0} port = {1}", new Object[] {receiverAddress, contactPort});

					clientSock = messageProcessor
							.blockingConnect(new InetSocketAddress(receiverAddress, contactPort), 10000);

					newSocket = true;

					messageChannel.peerPort = contactPort;
					putSocket(key, clientSock);
				}

				logger.log(Level.FINEST, "sending to {0}", key);
			} else {
				logger.log(Level.SEVERE, "IOException occured at", ex);

				throw ex;
			}

			return clientSock;
		} finally {
			try {
				if(clientSock != null) {
					if(newSocket && messageChannel instanceof NioTlsMessageChannel) {
						// We don't write data when using TLS, the new socket needs to handshake first
					} else {
						writeChunks(clientSock, bytes, length);
					}
				}
			} finally {
				keyedSemaphore.leaveIOCriticalSection(key);
			}
		}

		if(clientSock == null) {
			logger.log(Level.SEVERE, "{0}\n Could not connect to {1}:{2}", new Object[] { this.socketTable,
					receiverAddress, contactPort});

			throw new IOException("Could not connect to " + receiverAddress + ":" + contactPort);
		} else {
			return clientSock;
		}
	}

	/**
	 * Close all the cached connections.
	 */
	public void closeAll() {
		logger.log(Level.FINEST, "Closing {0} sockets from IOHandler", socketTable.size());

		for(Enumeration<SocketChannel> values = socketTable.elements(); values.hasMoreElements();) {
			SocketChannel s = values.nextElement();

			try {
				s.close();
			} catch(IOException ex) {
				logger.log(Level.FINEST, "silently ignoring exception", ex);
			}
		}
	}

	public void stop() {
		try {
			// Reworked the method for https://java.net/jira/browse/JSIP-471
			logger.log(Level.FINEST, "keys to check for inactivity removal {0}",
					NioTcpMessageChannel.channelMap.keySet());
			logger.log(Level.FINEST, "existing socket in NIOHandler {0}", socketTable.keySet());

			Iterator<Entry<SocketChannel, NioTcpMessageChannel>> entriesIterator = NioTcpMessageChannel.channelMap
					.entrySet().iterator();
			while (entriesIterator.hasNext()) {
				Entry<SocketChannel, NioTcpMessageChannel> entry = entriesIterator.next();
				SocketChannel socketChannel = entry.getKey();
				NioTcpMessageChannel messageChannel = entry.getValue();

				logger.log(Level.FINEST, "stop() : Removing socket {0} socketChannel = {1}", new Object[] {
						messageChannel.key, socketChannel});

				messageChannel.close();

				entriesIterator = NioTcpMessageChannel.channelMap.entrySet().iterator();
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public SocketChannel createOrReuseSocket(InetAddress inetAddress, int port) throws IOException {
		String key = NIOHandler.makeKey(inetAddress, port);
		SocketChannel channel = null;
		keyedSemaphore.enterIOCriticalSection(key);
		try {
			channel = getSocket(key);

			if(channel != null && (!channel.isConnected() || !channel.isOpen())) {
				logger.log(Level.FINEST, "Channel disconnected {0}", channel);

				channel = null;
			}

			// this is where the threads will race
			if(channel == null) {
				SocketAddress sockAddr = new InetSocketAddress(inetAddress, port);

				channel = messageProcessor.blockingConnect((InetSocketAddress) sockAddr, 10000);

				logger.log(Level.FINEST, "create channel = {0} {1} {2}", new Object[] {channel, inetAddress, port});

				if(channel != null && channel.isConnected()) {
					putSocket(NIOHandler.makeKey(inetAddress, port), channel);

					logger.log(Level.FINEST, "channel cached channel = {0}", channel);
				}
			}

			return channel;
		} finally {
			keyedSemaphore.leaveIOCriticalSection(key);

			logger.log(Level.FINEST, "Returning socket: {0} channel: {1}", new Object[] {key, channel});
		}
	}
}
