package gov.nist.javax.sip.stack.sctp;

import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.ParseExceptionListener;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

/**
 * SCTP message channel
 */
final class SCTPMessageChannel extends MessageChannel
		implements ParseExceptionListener, Comparable<SCTPMessageChannel> {
	private static final Logger logger = Logger.getLogger(SCTPMessageChannel.class.getName());

	private final SCTPMessageProcessor processor;
	private InetSocketAddress peerAddress; // destination address
	private InetSocketAddress peerSrcAddress;

	private final SctpChannel channel;
	private final SelectionKey key;

	private final MessageInfo messageInfo; // outgoing SCTP options, cached
	private long rxTime; // < Time first byte of message was received

	private final ByteBuffer rxBuffer = ByteBuffer.allocateDirect(10000);

	private final StringMsgParser parser = new StringMsgParser(); // Parser instance

	// for outgoing connections
	SCTPMessageChannel(SCTPMessageProcessor p, InetSocketAddress dest) throws IOException {
		this.processor = p;
		this.messageProcessor = p; // super class
		this.peerAddress = dest;
		this.peerSrcAddress = dest; // assume the same, override upon packet

		this.messageInfo = MessageInfo.createOutgoing(dest, 0);
		messageInfo.unordered(true);

		this.channel = SctpChannel.open(dest, 1, 1);
		channel.configureBlocking(false);
		this.key = processor.registerChannel(this, channel);
	}

	// For incoming connections
	SCTPMessageChannel(SCTPMessageProcessor p, SctpChannel c) throws IOException {
		this.processor = p;
		this.messageProcessor = p; // super class
		SocketAddress a = c.getRemoteAddresses().iterator().next();
		this.peerAddress = (InetSocketAddress) a;
		this.peerSrcAddress = (InetSocketAddress) a;
		this.messageInfo = MessageInfo.createOutgoing(a, 0);
		messageInfo.unordered(true);

		this.channel = c;
		channel.configureBlocking(false);
		this.key = processor.registerChannel(this, channel);
	}

	@Override
	public void close() {
		try {
			this.key.cancel();
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			processor.removeChannel(this);
		}
	}

	void closeNoRemove() {
		try {
			this.key.cancel();
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getKey() {
		// Note: could put this in super class
		return getKey(this.getPeerInetAddress(), this.getPeerPort(), this.getTransport());
	}

	@Override
	public String getPeerAddress() {
		return peerAddress.getHostString();
	}

	@Override
	protected InetAddress getPeerInetAddress() {
		return peerAddress.getAddress();
	}

	@Override
	public InetAddress getPeerPacketSourceAddress() {
		return peerSrcAddress.getAddress();
	}

	@Override
	public int getPeerPacketSourcePort() {
		return peerSrcAddress.getPort();
	}

	@Override
	public int getPeerPort() {
		return peerAddress.getPort();
	}

	@Override
	protected String getPeerProtocol() {
		return "sctp"; // else something really is weird ;)
	}

	@Override
	public SIPTransactionStack getSIPStack() {
		return processor.getSIPStack();
	}

	@Override
	public String getTransport() {
		return "sctp";
	}

	@Override
	public String getViaHost() {
		return processor.getSavedIpAddress();
	}

	@Override
	public int getViaPort() {
		return processor.getPort();
	}

	@Override
	public boolean isReliable() {
		return true;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void sendMessage(SIPMessage sipMessage) throws IOException {
		byte[] msg = sipMessage.encodeAsBytes(this.getTransport());
		this.sendMessage(msg, this.getPeerInetAddress(), this.getPeerPort(), false);
	}

	@Override
	protected void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean reconnectFlag)
			throws IOException {

		assert (receiverAddress.equals(peerAddress.getAddress()));
		assert (receiverPort == peerAddress.getPort());

		// ignoring 'reconnect' for now
		int nBytes = channel.send(ByteBuffer.wrap(message), messageInfo);

		logger.log(Level.FINEST, "SCTP bytes sent: {0}", nBytes);
	}

	/**
	 * Called by SCTPMessageProcessor when one or more bytes are available for
	 * reading
	 * 
	 * @throws IOException
	 */
	void readMessages() throws IOException {
		if(rxTime == 0) {
			rxTime = System.currentTimeMillis();
		}

		MessageInfo info = channel.receive(rxBuffer, null, null);
		if(info == null) {
			// happens a lot, some sort of keep-alive?
			logger.log(Level.FINEST, "SCTP read-event but no message");

			return;
		} else if(info.bytes() == -1) {
			logger.log(Level.WARNING, "SCTP peer closed, closing too...");

			this.close();

			return;
		} else if(!info.isComplete()) {
			logger.log(Level.FINEST, "SCTP incomplete message; bytes= {0}", info.bytes());

			return;
		} else {
			logger.log(Level.FINEST, "SCTP message now complete; bytes= {0}", info.bytes());
		}

		// Assume it is 1 full message, not multiple messages
		byte[] msg = new byte[rxBuffer.position()];

		rxBuffer.flip();
		rxBuffer.get(msg);
		rxBuffer.compact();

		try {
			SIPMessage m = parser.parseSIPMessage(msg, true, true, this);

			this.processMessage(m, rxTime);

			// reset for next message
			rxTime = 0;
		} catch(ParseException e) {
			logger.log(Level.FINEST, "Invalid message bytes={0}:{1}", new Object[] {msg.length, new String(msg)});

			this.close();

			throw new IOException("Error parsing incoming SCTP message", e);
		}
	}

	/**
	 * Actually process the parsed message.
	 * 
	 * @param sipMessage copied from UDPMessageChannel,
	 */
	private void processMessage(SIPMessage sipMessage, long rxTime) {
		SIPTransactionStack sipStack = processor.getSIPStack();

		sipMessage.setRemoteAddress(this.peerAddress.getAddress());
		sipMessage.setRemotePort(this.getPeerPort());
		sipMessage.setLocalPort(this.getPort());
		sipMessage.setLocalAddress(this.getMessageProcessor().getIpAddress());

		if(sipMessage instanceof SIPRequest) {
			SIPRequest sipRequest = (SIPRequest) sipMessage;

			// This is a request - process it. So far so good -- we will commit this message if all processing is OK.
			logger.log(Level.FINEST, "sipMessage: {0}, peerHostPort: {1}, host: {2}, port: {3}, rxTime: {4}",
					new Object[] { sipMessage, this.getPeerHostPort(), this.getHost(), this.getPort(), rxTime});

			ServerRequestInterface sipServerRequest = sipStack.newSIPServerRequest(sipRequest, this);
			// Drop it if there is no request returned
			if(sipServerRequest == null) {
				logger.log(Level.WARNING, "Null request interface returned -- dropping request");

				return;
			}

			logger.log(Level.FINEST, "About to process {0}/{1}",
					new Object[] {sipRequest.getFirstLine(), sipServerRequest});

			try {
				sipServerRequest.processRequest(sipRequest, this);
			} finally {
				if (sipServerRequest instanceof SIPTransaction) {
					SIPServerTransaction sipServerTx = (SIPServerTransaction) sipServerRequest;
					if (!sipServerTx.passToListener()) {
						((SIPTransaction) sipServerRequest).releaseSem();
					}
				}
			}

			logger.log(Level.FINEST, "Done processing {0}/{1}",
					new Object[] {sipRequest.getFirstLine(), sipServerRequest});
			// So far so good -- we will commit this message if all processing is OK.
		} else {
			// Handle a SIP Reply message.
			SIPResponse sipResponse = (SIPResponse) sipMessage;

			try {
				sipResponse.checkHeaders();
			} catch(ParseException ex) {
				logger.log(Level.SEVERE, "Dropping Badly formatted response message >>> {0}", sipResponse);

				return;
			}

			ServerResponseInterface sipServerResponse = sipStack.newSIPServerResponse(sipResponse, this);
			if(sipServerResponse != null) {
				try {
					if(sipServerResponse instanceof SIPClientTransaction
							&& !((SIPClientTransaction) sipServerResponse).checkFromTag(sipResponse)) {
						logger.log(Level.SEVERE, "Dropping response message with invalid tag >>> {0}", sipResponse);

						return;
					}

					sipServerResponse.processResponse(sipResponse, this);
				} finally {
					if (sipServerResponse instanceof SIPTransaction
							&& !((SIPTransaction) sipServerResponse).passToListener())
						((SIPTransaction) sipServerResponse).releaseSem();
				}

				// Normal processing of message.
			} else {
				logger.log(Level.FINEST, "null sipServerResponse!");
			}
		}
	}

	/**
	 * Implementation of the ParseExceptionListener interface.
	 *
	 * @param ex Exception that is given to us by the parser.
	 * @throws ParseException If we choose to reject the header or message.
	 *
	 * copied from UDPMessageChannel
	 */
	public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message)
			throws ParseException {
		logger.log(Level.SEVERE, ex.getMessage(), ex);

		// Log the bad message for later reference.
		if((hdrClass != null) && (hdrClass.equals(From.class) || hdrClass.equals(To.class)
				|| hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class)
				|| hdrClass.equals(RequestLine.class) || hdrClass.equals(StatusLine.class))) {
			logger.log(Level.SEVERE, "BAD MESSAGE! message:\n{0}", message);

			throw ex;
		} else {
			sipMessage.addUnparsed(header);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SCTPMessageChannel)) {
			return false;
		}

		return 0 == compareTo((SCTPMessageChannel) obj);
	}

	@Override
	public int compareTo(SCTPMessageChannel o) {
		return this.hashCode() - o.hashCode();
	}

	@Override
	protected void uncache() {
		processor.removeChannel(this);
	}
}
