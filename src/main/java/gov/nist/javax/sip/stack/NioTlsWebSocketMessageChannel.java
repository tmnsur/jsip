package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SSLStateMachine.MessageSendCallback;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;
import javax.sip.address.SipURI;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioTlsWebSocketMessageChannel extends NioWebSocketMessageChannel implements NioTlsChannelInterface {
	private static final Logger logger = Logger.getLogger(NioTlsWebSocketMessageChannel.class.getName());

	SSLStateMachine sslStateMachine;

	private int appBufferMax;
	private int netBufferMax;

	public static NioTlsWebSocketMessageChannel create(NioTlsWebSocketMessageProcessor nioTcpMessageProcessor,
			SocketChannel socketChannel) throws IOException {
		NioTlsWebSocketMessageChannel retval = (NioTlsWebSocketMessageChannel) channelMap.get(socketChannel);

		if (null == retval) {
			retval = new NioTlsWebSocketMessageChannel(nioTcpMessageProcessor, socketChannel);

			channelMap.put(socketChannel, retval);
		}

		return retval;
	}

	protected NioTlsWebSocketMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel socketChannel)
			throws IOException {
		super(nioTcpMessageProcessor, socketChannel);

		messageProcessor = nioTcpMessageProcessor;
		myClientInputStream = socketChannel.socket().getInputStream();

		try {
			this.init(false);

			createBuffers();
		} catch(Exception e) {
			throw new IOException("Can't do TLS init", e);
		}
	}

	public void init(boolean clientMode) throws Exception {
		SSLContext ctx = clientMode ? ((NioTlsWebSocketMessageProcessor) messageProcessor).sslClientCtx
				: ((NioTlsWebSocketMessageProcessor) messageProcessor).sslServerCtx;
		sslStateMachine = new SSLStateMachine(ctx.createSSLEngine(), this);

		sslStateMachine.sslEngine.setUseClientMode(clientMode);

		sslStateMachine.sslEngine.setNeedClientAuth(false);
		sslStateMachine.sslEngine.setWantClientAuth(false);

		String clientProtocols = ((SipStackImpl) super.sipStack).getConfigurationProperties()
				.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
		if (clientProtocols != null) {
			sslStateMachine.sslEngine.setEnabledProtocols(clientProtocols.split(","));
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

		ByteBuffer b = ByteBuffer.wrap(NioWebSocketMessageChannel.wrapBufferIntoWebSocketFrame(msg, client));
		try {
			sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
					new MessageSendCallback() {

						@Override
						public void doSend(byte[] bytes) throws IOException {
							NioTlsWebSocketMessageChannel.super.sendNonWebSocketMessage(bytes, isClient);
						}
					});
		} catch (Exception e) {
			throw new IOException("Can't send message", e);
		}
	}

	public void sendEncryptedData(byte[] msg) throws IOException {
		// bypass the encryption for already encrypted data or TLS metadata
		logger.log(Level.FINEST, "sendEncryptedData this: {0}, peerPort: {1}, addr: {2}",
				new Object[] { this, peerPort, peerAddress });

		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		super.sendNonWebSocketMessage(msg, false);
	}

	@Override
	public void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		checkSocketState();

		ByteBuffer b = ByteBuffer.wrap(NioWebSocketMessageChannel.wrapBufferIntoWebSocketFrame(message, client));

		sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
				new MessageSendCallback() {
					@Override
					public void doSend(byte[] bytes) throws IOException {
						NioTlsWebSocketMessageChannel.super.sendTCPMessage(bytes, receiverAddress, receiverPort, retry);
					}
				}
		);
	}

	@Override
	public void sendMessage(final SIPMessage sipMessage, final InetAddress receiverAddress, final int receiverPort)
			throws IOException {
		if(sipMessage instanceof SIPRequest && client && !httpClientRequestSent) {
			httpClientRequestSent = true;
			SIPRequest request = (SIPRequest) sipMessage;
			SipURI requestUri = (SipURI) request.getRequestURI();
			this.httpHostHeader = requestUri.getHeader("Host");
			this.httpLocation = requestUri.getHeader("Location");
			this.httpMethod = requestUri.getMethodParam();
			final String http = this.httpMethod + " " + this.httpLocation + " HTTP/1.1\r\n" + "Host: "
					+ this.httpHostHeader + "\r\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n"
					+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" + "Sec-WebSocket-Protocol: sip\r\n"
					+ "Sec-WebSocket-Version: 13\r\n\r\n";

			ByteBuffer b = ByteBuffer.wrap(http.getBytes());

			sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
					new MessageSendCallback() {
						@Override
						public void doSend(byte[] bytes) throws IOException {
							NioTlsWebSocketMessageChannel.super.sendTCPMessage(bytes, receiverAddress, receiverPort,
									false);

							byte[] wsM = sipMessage.toString().getBytes();
							byte[] wsMessage = NioWebSocketMessageChannel.wrapBufferIntoWebSocketFrame(wsM, client);
							ByteBuffer b = ByteBuffer.wrap(wsMessage);

							sslStateMachine.wrap(b, ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
									new MessageSendCallback() {
										@Override
										public void doSend(byte[] bytes) throws IOException {
											NioTlsWebSocketMessageChannel.super.sendTCPMessage(bytes, receiverAddress,
													receiverPort, false);
										}
									}
							);
						}
					});
		} else {
			// https://java.net/jira/browse/JSIP-497 fix transport for WSS
			sendMessage(sipMessage.encodeAsBytes(this.getTransport()), this.client);
		}
	}

	public void sendHttpMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		checkSocketState();

		sslStateMachine.wrap(ByteBuffer.wrap(message), ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
				new MessageSendCallback() {
					@Override
					public void doSend(byte[] bytes) throws IOException {
						NioTlsWebSocketMessageChannel.super.sendMessage(bytes, receiverAddress, receiverPort, retry);
					}
				}
		);
	}

	private void createBuffers() {
		SSLSession session = sslStateMachine.sslEngine.getSession();
		appBufferMax = session.getApplicationBufferSize();
		netBufferMax = session.getPacketBufferSize();

		logger.log(Level.FINEST, "appBufferMax: {0}, netBufferMax: {1}", new Object[] {appBufferMax, netBufferMax});
	}

	public NioTlsWebSocketMessageChannel(InetAddress inetAddress, int port, SIPTransactionStack sipStack,
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
		logger.log(Level.FINEST, "Adding WSS bytes for decryption: {0}", bytes.length);

		if(bytes.length <= 0) {
			return;
		}

		sslStateMachine.unwrap(ByteBuffer.wrap(bytes));
	}

	@Override
	protected void sendNonWebSocketMessage(byte[] msg, final boolean isClient) throws IOException {
		logger.log(Level.FINEST, "sendMessage isClient: {0}, this: {1}", new Object[] {isClient, this});

		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		checkSocketState();

		sslStateMachine.wrap(ByteBuffer.wrap(msg), ByteBufferFactory.getInstance().allocateDirect(netBufferMax),
				new MessageSendCallback() {
					@Override
					public void doSend(byte[] bytes) throws IOException {
						NioTlsWebSocketMessageChannel.super.sendTCPMessage(bytes, peerAddress, peerPort, isClient);
					}
				}
		);
	}

	@Override
	public String getTransport() {
		return this.messageProcessor.transport;
	}

	@Override
	public void onNewSocket(byte[] message) {
		super.onNewSocket(message);
		try {
			if(message != null) {
				logger.log(Level.FINEST, "New socket for: {0} last message: {1}", new Object[] {this,
						new String(message, StandardCharsets.UTF_8)});
			}

			init(true);
			createBuffers();
			sendMessage(message, false);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Cant reinit", e);
		}
	}

	private void checkSocketState() throws IOException {
		if(socketChannel != null && (!socketChannel.isConnected() || !socketChannel.isOpen())) {
			logger.log(Level.FINEST, "Need to reset SSL engine for socket: {0}", socketChannel);

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
		super.addBytes(bytes);
	}

	@Override
	public SipStackImpl getSIPStack() {
		return (SipStackImpl) super.getSIPStack();
	}
}
