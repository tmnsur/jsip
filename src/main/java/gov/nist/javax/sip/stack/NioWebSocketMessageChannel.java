package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

public class NioWebSocketMessageChannel extends NioTcpMessageChannel {
	private static final Logger logger = Logger.getLogger(NioWebSocketMessageChannel.class.getName());

	private WebSocketCodec codec = new WebSocketCodec(true, true);

	volatile boolean readingHttp = true;
	String httpInput = "";
	boolean client;
	boolean httpClientRequestSent;
	String httpHostHeader;
	String httpMethod;
	String httpLocation;

	public static NioWebSocketMessageChannel create(NioWebSocketMessageProcessor nioTcpMessageProcessor,
			SocketChannel socketChannel) throws IOException {
		NioWebSocketMessageChannel retval = (NioWebSocketMessageChannel) channelMap.get(socketChannel);

		if(null == retval) {
			retval = new NioWebSocketMessageChannel(nioTcpMessageProcessor, socketChannel);

			channelMap.put(socketChannel, retval);
		}

		return retval;
	}

	protected NioWebSocketMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel socketChannel)
			throws IOException {
		super(nioTcpMessageProcessor, socketChannel);

		messageProcessor = nioTcpMessageProcessor;
		myClientInputStream = socketChannel.socket().getInputStream();
	}

	@Override
	protected void sendMessage(final byte[] msg, final boolean isClient) throws IOException {
		logger.log(Level.FINEST, "sendMessage isClient: {0}, this: {1}", new Object[] {isClient, this});

		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		sendWrapped(msg, this.peerAddress, this.peerPort, isClient);
	}

	protected void sendNonWebSocketMessage(byte[] msg, boolean isClient) throws IOException {
		logger.log(Level.FINEST, "sendMessage isClient: {0}, this: {1}", new Object[] {isClient, this});
	
		lastActivityTimeStamp = System.currentTimeMillis();

		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;

		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}

		super.sendTCPMessage(msg, this.peerAddress, this.peerPort, isClient);
	}

	public static byte[] wrapBufferIntoWebSocketFrame(byte[] buffer, boolean client) {
		try {
			return WebSocketCodec.encode(buffer, 0, true, client);
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return null;
	}

	public void sendWrapped(byte message[], InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		message = wrapBufferIntoWebSocketFrame(message, client);

		super.sendTCPMessage(message, receiverAddress, receiverPort, retry);
	}

	@Override
	public void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		sendWrapped(message, receiverAddress, receiverPort, retry);
	}

	@Override
	public void sendMessage(SIPMessage sipMessage, InetAddress receiverAddress, int receiverPort) throws IOException {
		if(sipMessage instanceof SIPRequest) {
			if(client && !httpClientRequestSent) {
				httpClientRequestSent = true;

				SIPRequest request = (SIPRequest) sipMessage;

				SipURI requestUri = (SipURI) request.getRequestURI();

				this.httpHostHeader = requestUri.getHeader("Host");
				this.httpLocation = requestUri.getHeader("Location");
				this.httpMethod = requestUri.getMethodParam();

				String http = this.httpMethod + " " + this.httpLocation + " HTTP/1.1\r\n" + "Host: "
						+ this.httpHostHeader + "\r\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n"
						+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" + "Sec-WebSocket-Protocol: sip\r\n"
						+ "Sec-WebSocket-Version: 13\r\n\r\n";

				super.sendTCPMessage(http.getBytes(), receiverAddress, receiverPort, false);

				try {
					Thread.sleep(500);
				} catch(InterruptedException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}

		super.sendMessage(sipMessage, receiverAddress, receiverPort);
	}

	public NioWebSocketMessageChannel(InetAddress inetAddress, int port, SIPTransactionStack sipStack,
			NioTcpMessageProcessor nioTcpMessageProcessor) throws IOException {
		super(inetAddress, port, sipStack, nioTcpMessageProcessor);
		client = true;
		this.codec = new WebSocketCodec(false, true);
	}

	@Override
	protected void addBytes(byte[] bytes) throws Exception {
		String s = new String(bytes);

		if (readingHttp) {
			byte[] remaining = null;
			for (int q = 0; q < bytes.length - 3; q++) {
				if (bytes[q] == '\r' && bytes[q + 1] == '\n' && bytes[q + 2] == '\r' && bytes[q + 3] == '\n') {
					s = s.substring(0, q + 4);
					remaining = new byte[bytes.length - q - 4];
					for (int w = 0; w < remaining.length; w++) {
						remaining[w] = bytes[q + 4 + w];
					}
				}
			}
			httpInput += s;
			if (s.endsWith("\r\n") || s.endsWith("\n")) {
				readingHttp = false;
				if(!httpInput.startsWith("HTTP")) {
					byte[] response = new WebSocketHttpHandshake().createHttpResponse(s);

					sendNonWebSocketMessage(response, false);
				} else {
					logger.log(Level.FINEST, "HTTP Response. We are websocket client.\n{0}", httpInput);
				}
			}

			if(remaining != null) {
				addBytes(remaining);
			}
		} else if(!readingHttp) {
			ByteArrayInputStream bios = new ByteArrayInputStream(bytes);

			byte[] decodedMsg = null;

			do {
				decodedMsg = codec.decode(bios);

				// Chrome waits for us to close the socket when it sends a close opcode
				// https://code.google.com/p/chromium/issues/detail?id=388243#c15
				if(codec.isCloseOpcodeReceived()) {
					logger.log(Level.FINEST, "Websocket close, sending polite close response");

					ByteBuffer byteBuff = ByteBuffer.wrap(new byte[] { (byte) 0x88, (byte) 0x00 });

					socketChannel.write(byteBuff);

					return;
				}

				if(null == decodedMsg) {
					// the codec can't parse a full websocket frame, we will try again when have more data
					return;
				}

				nioParser.addBytes(decodedMsg);

				logger.log(Level.FINEST, "Nio websocket bytes were added: {0}", decodedMsg.length);
			} while(true);
		}
	}

	@Override
	public String getTransport() {
		return this.messageProcessor.transport;
	}

	@Override
	public void onNewSocket(byte[] message) {
		super.onNewSocket(message);

	}

	/**
	 * Call back method. When the parser finshes parsing a message let the channel
	 * see it and decide if it want to create some address mappings (for Websocket
	 * or otherwise).
	 * 
	 * @param message
	 * @throws Exception
	 */
	@Override
	public void processMessage(SIPMessage message) throws Exception {
		if(message instanceof Request) {
			ContactHeader contact = (ContactHeader) message.getHeader(ContactHeader.NAME);
			RecordRouteHeader rr = (RecordRouteHeader) message.getHeader(RecordRouteHeader.NAME);
			ViaHeader via = message.getTopmostViaHeader();

			if(rr == null) {
				if(contact != null) {
					rewriteUri((SipURI) contact.getAddress().getURI());
				}
			} else {
				// not needed but just in case some clients does it
				rewriteUri((SipURI) rr.getAddress().getURI());
			}

			String viaHost = via.getHost();
			if(viaHost.endsWith(".invalid")) {
				via.setHost(getPeerAddress());
				via.setPort(getPeerPort());
			}
		} else {
			ContactHeader contact = (ContactHeader) message.getHeader(ContactHeader.NAME);

			if(contact != null) {
				rewriteUri((SipURI) contact.getAddress().getURI());
			}
		}

		super.processMessage(message);
	}

	public void rewriteUri(SipURI uri) {
		try {
			String uriHost = uri.getHost();

			if(uriHost.endsWith(".invalid")) {
				uri.setHost(getPeerAddress());
			}
		} catch(ParseException e) {
			logger.log(Level.SEVERE, "Cant parse address", e);
		}

		uri.setPort(getPeerPort());
	}
}
