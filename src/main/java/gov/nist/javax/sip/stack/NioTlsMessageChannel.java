package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.SSLStateMachine.MessageSendCallback;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioTlsMessageChannel extends NioTcpMessageChannel implements NioTlsChannelInterface {
	private static final Logger logger = Logger.getLogger(NioTlsMessageChannel.class.getName());

	SSLStateMachine sslStateMachine;

	// Added for https://java.net/jira/browse/JSIP-483
	private HandshakeCompletedListener handshakeCompletedListener;
	private boolean handshakeCompleted = false;

	private int appBufferMax;
	private int netBufferMax;

	public static NioTcpMessageChannel create(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel socketChannel) throws IOException {
		NioTcpMessageChannel retval = channelMap.get(socketChannel);

		if (retval == null) {
			retval = new NioTlsMessageChannel(nioTcpMessageProcessor, socketChannel);

			channelMap.put(socketChannel, retval);
		}

		return retval;
	}

	protected NioTlsMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel socketChannel)
			throws IOException {
		super(nioTcpMessageProcessor, socketChannel);

		messageProcessor = nioTcpMessageProcessor;
		myClientInputStream = socketChannel.socket().getInputStream();

		try {
			init(false);
			createBuffers();
		} catch (Exception e) {
			throw new IOException("Can't do TLS init", e);
		}
	}

	public void init(boolean clientMode) throws Exception {
		SSLContext ctx = clientMode ? ((NioTlsMessageProcessor) messageProcessor).sslClientCtx
				: ((NioTlsMessageProcessor) messageProcessor).sslServerCtx;
		sslStateMachine = new SSLStateMachine(ctx.createSSLEngine(), this);

		sslStateMachine.sslEngine.setUseClientMode(clientMode);
		String auth = ((SipStackImpl) super.sipStack).getConfigurationProperties()
				.getProperty("gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE");
		if (auth == null) {
			auth = "Enabled";
		}
		if (auth.equals("Disabled") || auth.equals("DisabledAll")) {
			sslStateMachine.sslEngine.setNeedClientAuth(false);
			sslStateMachine.sslEngine.setWantClientAuth(false);
		} else if (auth.equals("Enabled")) {
			sslStateMachine.sslEngine.setNeedClientAuth(true);
		} else if (auth.equals("Want")) {
			sslStateMachine.sslEngine.setNeedClientAuth(false);
			sslStateMachine.sslEngine.setWantClientAuth(true);
		} else {
			throw new IllegalStateException("Invalid parameter for TLS authentication: " + auth);
		}

		// http://java.net/jira/browse/JSIP-451
		sslStateMachine.sslEngine.setEnabledProtocols(((SipStackImpl) sipStack).getEnabledProtocols());

		// Added for https://java.net/jira/browse/JSIP-483
		if(getHandshakeCompletedListener() == null) {
			setHandshakeCompletedListener(new HandshakeCompletedListenerImpl(this, getSocketChannel()));
		}
	}

	@Override
	public ByteBuffer prepareEncryptedDataBuffer() {
		return ByteBufferFactory.getInstance().allocateDirect(netBufferMax);
	}

	@Override
	public ByteBuffer prepareAppDataBuffer() {
		return ByteBufferFactory.getInstance().allocateDirect(appBufferMax);
	}

	@Override
	public ByteBuffer prepareAppDataBuffer(int capacity) {
		return ByteBufferFactory.getInstance().allocateDirect(capacity);
	}

	public static class SSLReconnectedException extends IOException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	protected void sendMessage(final byte[] msg, final boolean isClient) throws IOException {
		checkSocketState();

		ByteBuffer b = ByteBuffer.wrap(msg);
		try {
			sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
					new MessageSendCallback() {
						@Override
						public void doSend(byte[] bytes) throws IOException {
							NioTlsMessageChannel.super.sendMessage(bytes, isClient);
						}
					});
		} catch(Exception e) {
			throw new IOException("Can't send message", e);
		}
	}

	@Override
	public void sendEncryptedData(byte[] msg) throws IOException {
		// bypass the encryption for already encrypted data or TLS metadata
		logger.log(Level.FINEST, "sendEncryptedData this: {0}, peerPort: {1} addr: {2}",
				new Object[] { this, peerPort, peerAddress });

		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		super.sendMessage(msg, this.peerAddress, this.peerPort, true);
	}

	@Override
	public void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		checkSocketState();

		ByteBuffer b = ByteBuffer.wrap(message);
		try {
			sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
					new MessageSendCallback() {

						@Override
						public void doSend(byte[] bytes) throws IOException {
							NioTlsMessageChannel.super.sendMessage(bytes, receiverAddress, receiverPort, retry);

						}
					});
		} catch(IOException e) {
			throw e;
		}
	}

	protected void createBuffers() {
		SSLSession session = sslStateMachine.sslEngine.getSession();

		appBufferMax = session.getApplicationBufferSize();
		netBufferMax = session.getPacketBufferSize();

		logger.log(Level.FINEST, "appBufferMax: {0}, netBufferMax: {1}", new Object[] {appBufferMax, netBufferMax});
	}

	public NioTlsMessageChannel(InetAddress inetAddress, int port, SIPTransactionStack sipStack,
			NioTcpMessageProcessor nioTcpMessageProcessor) throws IOException {
		super(inetAddress, port, sipStack, nioTcpMessageProcessor);

		try {
			init(true);

			createBuffers();
		} catch(Exception e) {
			throw new IOException("Can't init the TLS channel", e);
		}
	}

	@Override
	protected void addBytes(byte[] bytes) throws Exception {
		logger.log(Level.FINEST, "Adding TLS bytes for decryption: {0}", bytes.length);

		if(bytes.length <= 0) {
			return;
		}

		sslStateMachine.unwrap(ByteBuffer.wrap(bytes));
	}

	@Override
	public String getTransport() {
		return "TLS";
	}

	@Override
	public void onNewSocket(byte[] message) {
		super.onNewSocket(message);
		try {
			String last = null;

			if(null != message) {
				last = new String(message, StandardCharsets.UTF_8);
			}

			logger.log(Level.FINEST, "New socket for: {0} last message: {1}", new Object[] {this, last});

			init(true);
			createBuffers();

			sendMessage(message, false);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Cant reinit", e);
		}
	}

	private void checkSocketState() throws IOException {
		if(socketChannel != null && (!socketChannel.isConnected() || !socketChannel.isOpen())) {
			logger.log(Level.FINEST, "Need to reset SSL engine for socket {0}", socketChannel);

			try {
				init(sslStateMachine.sslEngine.getUseClientMode());
			} catch(Exception ex) {
				throw new IOException(ex);
			}
		}
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public void addPlaintextBytes(byte[] bytes) throws Exception {
		nioParser.addBytes(bytes);
	}

	// Methods below Added for https://java.net/jira/browse/JSIP-483
	public void setHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListenerImpl) {
		this.handshakeCompletedListener = handshakeCompletedListenerImpl;
	}

	/**
	 * @return the handshakeCompletedListener
	 */
	public HandshakeCompletedListenerImpl getHandshakeCompletedListener() {
		return (HandshakeCompletedListenerImpl) handshakeCompletedListener;
	}

	/**
	 * @return the handshakeCompleted
	 */
	public boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	/**
	 * @param handshakeCompleted the handshakeCompleted to set
	 */
	public void setHandshakeCompleted(boolean handshakeCompleted) {
		this.handshakeCompleted = handshakeCompleted;
	}

	@Override
	public SipStackImpl getSIPStack() {
		return (SipStackImpl) super.getSIPStack();
	}
}
