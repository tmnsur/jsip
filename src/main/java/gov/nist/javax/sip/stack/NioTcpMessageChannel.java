package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.NioPipelineParser;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

public class NioTcpMessageChannel extends ConnectionOrientedMessageChannel {
	private static final Logger logger = Logger.getLogger(NioTcpMessageChannel.class.getName());

	protected static HashMap<SocketChannel, NioTcpMessageChannel> channelMap = new HashMap<>();

	protected SocketChannel socketChannel;
	protected long lastActivityTimeStamp;
	NioPipelineParser nioParser = null;

	public static NioTcpMessageChannel create(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel socketChannel) throws IOException {
		NioTcpMessageChannel retval = channelMap.get(socketChannel);
		if (retval == null) {
			retval = new NioTcpMessageChannel(nioTcpMessageProcessor, socketChannel);
			channelMap.put(socketChannel, retval);
		}
		retval.messageProcessor = nioTcpMessageProcessor;
		retval.myClientInputStream = socketChannel.socket().getInputStream();
		return retval;
	}

	public static NioTcpMessageChannel getMessageChannel(SocketChannel socketChannel) {
		return channelMap.get(socketChannel);
	}

	public static void putMessageChannel(SocketChannel socketChannel, NioTcpMessageChannel nioTcpMessageChannel) {
		channelMap.put(socketChannel, nioTcpMessageChannel);
	}

	public static void removeMessageChannel(SocketChannel socketChannel) {
		channelMap.remove(socketChannel);
	}

	public void readChannel() {
		logger.entering(NioTcpMessageChannel.class.getName(), "readChannel");

		int bufferSize = 4096;
		byte[] msg = new byte[bufferSize];
		this.isRunning = true;
		try {
			ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
			int nbytes = this.socketChannel.read(byteBuffer);
			byteBuffer.flip();
			msg = new byte[byteBuffer.remaining()];
			byteBuffer.get(msg);
			boolean streamError = nbytes == -1;
			nbytes = msg.length;
			byteBuffer.clear();

			logger.log(Level.FINEST, "Read {0} from socketChannel", nbytes);

			if (streamError) {
				throw new IOException("End-of-stream read (-1). "
						+ "This is usually an indication we are stuck and it is better to disconnect.");
			}

			/*
			 * This prevents us from getting stuck in a selector thread spin loop when
			 * socket is constantly ready for reading but there are no bytes.
			 */
			if (nbytes == 0) {
				throw new IOException("The socket is giving us empty TCP packets. "
						+ "This is usually an indication we are stuck and it is better to disconnect.");
			}

			// Otherwise just add the bytes to queue

			byte[] bytes = new byte[nbytes];
			System.arraycopy(msg, 0, bytes, 0, nbytes);
			addBytes(bytes);
			lastActivityTimeStamp = System.currentTimeMillis();

		} catch (Exception ex) { // https://java.net/jira/browse/JSIP-464 make sure to close connections on all
									// exceptions to avoid the stack to hang
			// Terminate the message.
			if (ex instanceof IOException && !(ex instanceof SSLException)) {
				try {
					nioParser.addBytes("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
				} catch (Exception e) {
					logger.log(Level.FINEST, "silently skipping exception", e);
				}
			}

			try {
				logger.log(Level.FINEST,
						"IOException closing sock {0} myAddress:myport {0}:{1}," + " remoteAddress:remotePort {2}:{3}",
						new Object[] { ex, myAddress, myPort, peerAddress, peerPort });

				close(true, false);
			} catch (Exception ex1) {
				logger.log(Level.FINEST, "Exception closing the socket", ex1);
			}
		}
	}

	// TLS will override here to add decryption
	protected void addBytes(byte[] bytes) throws Exception {
		nioParser.addBytes(bytes);
	}

	protected NioTcpMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel socketChannel)
			throws IOException {
		super(nioTcpMessageProcessor.getSIPStack());
		super.myClientInputStream = socketChannel.socket().getInputStream();
		try {
			this.peerAddress = socketChannel.socket().getInetAddress();
			this.peerPort = socketChannel.socket().getPort();
			this.socketChannel = socketChannel;
			super.mySock = socketChannel.socket();
			// messages that we write out to him.
			nioParser = new NioPipelineParser(sipStack, this, this.sipStack.getMaxMessageSize());
			this.peerProtocol = nioTcpMessageProcessor.transport;
			lastActivityTimeStamp = System.currentTimeMillis();
			super.key = MessageChannel.getKey(peerAddress, peerPort, nioTcpMessageProcessor.transport);

			myAddress = nioTcpMessageProcessor.getIpAddress().getHostAddress();
			myPort = nioTcpMessageProcessor.getPort();
		} finally {
			logger.log(Level.FINEST, "Done creating NioTcpMessageChannel {0} socketChannel: {1}",
					new Object[] { this, socketChannel });
		}
	}

	public NioTcpMessageChannel(InetAddress inetAddress, int port, SIPTransactionStack sipStack,
			NioTcpMessageProcessor nioTcpMessageProcessor) throws IOException {
		super(sipStack);

		logger.log(Level.FINEST, "NioTcpMessageChannel::NioTcpMessageChannel: {0}:{1}",
				new Object[] { inetAddress.getHostAddress(), port });

		try {
			messageProcessor = nioTcpMessageProcessor;
			// Take a cached socket to the destination, if none create a new one and cache
			// it
			socketChannel = nioTcpMessageProcessor.nioHandler.createOrReuseSocket(inetAddress, port);
			peerAddress = socketChannel.socket().getInetAddress();
			peerPort = socketChannel.socket().getPort();
			super.mySock = socketChannel.socket();
			peerProtocol = getTransport();
			nioParser = new NioPipelineParser(sipStack, this, this.sipStack.getMaxMessageSize());
			putMessageChannel(socketChannel, this);
			lastActivityTimeStamp = System.currentTimeMillis();
			super.key = MessageChannel.getKey(peerAddress, peerPort, getTransport());

			myAddress = nioTcpMessageProcessor.getIpAddress().getHostAddress();
			myPort = nioTcpMessageProcessor.getPort();

		} finally {
			logger.log(Level.FINEST, "NioTcpMessageChannel::NioTcpMessageChannel: Done creating"
					+ " NioTcpMessageChannel: {0}, socketChannel: {1}", new Object[] {this, socketChannel});
		}
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	@Override
	protected void close(boolean removeSocket, boolean stopKeepAliveTask) {
		try {
			logger.log(Level.FINEST, "Closing NioTcpMessageChannel: {0} socketChannel: {1}",
					new Object[] { this, socketChannel });

			removeMessageChannel(socketChannel);

			if(socketChannel != null) {
				socketChannel.close();
			}

			if(nioParser != null) {
				nioParser.close();
			}

			this.isRunning = false;

			if(removeSocket) {
				logger.log(Level.FINEST, "Removing NioTcpMessageChannel {0}, socketChannel: {1}",
						new Object[] {this, socketChannel});

				((NioTcpMessageProcessor) this.messageProcessor).nioHandler.removeSocket(socketChannel);
				((ConnectionOrientedMessageProcessor) this.messageProcessor).remove(this);
			}

			if(stopKeepAliveTask) {
				cancelPingKeepAliveTimeoutTaskIfStarted();
			}
		} catch(IOException e) {
			logger.log(Level.SEVERE, "Problem occured while closing", e);
		}
	}

	/**
	 * get the transport string.
	 * 
	 * @return TCP in this case.
	 */
	@Override
	public String getTransport() {
		return "TCP";
	}

	/**
	 * Send message to whoever is connected to us. Uses the topmost via address to
	 * send to.
	 * 
	 * @param msg is the message to send.
	 * @param isClient
	 */
	@Override
	protected void sendMessage(byte[] msg, boolean isClient) throws IOException {
		logger.log(Level.FINEST, "sendMessage isClient: {0}, this: {1}", new Object[] {isClient, this});

		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		sendTCPMessage(msg, this.peerAddress, this.peerPort, isClient);
	}

	/**
	 * Send a message to a specified address.
	 * 
	 * @param message         Pre-formatted message to send.
	 * @param receiverAddress Address to send it to.
	 * @param receiverPort    Receiver port.
	 * @throws IOException If there is a problem connecting or sending.
	 */
	public void sendMessage(byte message[], InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		sendTCPMessage(message, receiverAddress, receiverPort, retry);
	}

	/**
	 * Send a message to a specified address.
	 * 
	 * @param message         Pre-formatted message to send.
	 * @param receiverAddress Address to send it to.
	 * @param receiverPort    Receiver port.
	 * @throws IOException If there is a problem connecting or sending.
	 */
	public void sendTCPMessage(byte message[], InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		if(message == null || receiverAddress == null) {
			logger.log(Level.SEVERE, "receiverAddress = {0}", receiverAddress);

			throw new IllegalArgumentException("Null argument");
		}
		lastActivityTimeStamp = System.currentTimeMillis();

		if(peerPortAdvertisedInHeaders <= 0) {
			logger.log(Level.FINEST, "receiver port = {0} for this channel {1} key {2}",
					new Object[] {receiverPort, this, key});

			if(receiverPort <= 0) {
				// if port is 0 we assume the default port for TCP
				this.peerPortAdvertisedInHeaders = 5060;
			} else {
				this.peerPortAdvertisedInHeaders = receiverPort;
			}

			logger.log(Level.FINEST, "2.Storing peerPortAdvertisedInHeaders: {0} for this channel: {1} key: {2}",
					new Object[] {peerPortAdvertisedInHeaders, this, key});
		}

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;
		SocketChannel sock = nioHandler.sendBytes(this.messageProcessor.getIpAddress(), receiverAddress, receiverPort,
				"TCP", message, retry, this);

		if(sock != socketChannel && sock != null) {
			if(socketChannel != null) {
				logger.log(Level.WARNING, "[2] Old socket different than new socket on channel {0} {1} {2}",
						new Object[] {key, socketChannel, sock});
				logger.log(Level.WARNING, "Old socket local ip address {0}",
						socketChannel.socket().getLocalSocketAddress());
				logger.log(Level.WARNING, "Old socket remote ip address {0}",
						socketChannel.socket().getRemoteSocketAddress());
				logger.log(Level.WARNING, "New socket local ip address {0}", sock.socket().getLocalSocketAddress());
				logger.log(Level.WARNING, "New socket remote ip address {0}", sock.socket().getRemoteSocketAddress());

				// we can call socketChannel.close() directly but we better use the inherited method
				close(false, false);

				socketChannel = sock;
				putMessageChannel(socketChannel, this);

				onNewSocket(message);
			}

			if(socketChannel != null) {
				logger.log(Level.WARNING, "There was no exception for the retry mechanism so we keep going {0}", key);
			}

			socketChannel = sock;
		}
	}

	public void onNewSocket(byte[] message) {
		// nothing
	}

	/**
	 * Exception processor for exceptions detected from the parser. (This is invoked
	 * by the parser when an error is detected).
	 * 
	 * @param sipMessage -- the message that incurred the error.
	 * @param ex         -- parse exception detected by the parser.
	 * @param header     -- header that caused the error.
	 * @throws ParseException Thrown if we want to reject the message.
	 */
	public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message)
			throws ParseException {
		logger.log(Level.SEVERE, ex.getMessage(), ex);

		// Log the bad message for later reference.
		if((hdrClass != null) && (hdrClass.equals(From.class) || hdrClass.equals(To.class)
				|| hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class)
				|| hdrClass.equals(ContentLength.class) || hdrClass.equals(RequestLine.class)
				|| hdrClass.equals(StatusLine.class))) {
			logger.log(Level.FINEST, "Encountered Bad Message \n{0}", sipMessage);

			// send a 400 response for requests (except ACK) Currently only UDP
			String msgString = sipMessage.toString();
			if(!msgString.startsWith("SIP/") && !msgString.startsWith("ACK ")) {
				if(socketChannel != null) {
					logger.log(Level.SEVERE, "Malformed mandatory headers: closing socket!: {0}", socketChannel);

					try {
						socketChannel.close();
					} catch(IOException ie) {
						logger.log(Level.SEVERE, "Exception while closing socket!: {0}:{1}",
								new Object[] { socketChannel, ie });
					}
				}
			}

			throw ex;
		} else {
			sipMessage.addUnparsed(header);
		}
	}

	/**
	 * Equals predicate.
	 * 
	 * @param other is the other object to compare ourselves to for equals
	 */
	@Override
	public boolean equals(Object other) {
		if(null == other) {
			return false;
		}

		if(!this.getClass().equals(other.getClass())) {
			return false;
		}

		return this.socketChannel == ((NioTcpMessageChannel) other).socketChannel;
	}

	/**
	 * TCP Is not a secure protocol.
	 */
	@Override
	public boolean isSecure() {
		return false;
	}

	public long getLastActivityTimestamp() {
		return lastActivityTimeStamp;
	}
}
