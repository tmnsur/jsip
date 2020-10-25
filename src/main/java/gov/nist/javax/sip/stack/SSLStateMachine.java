package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * This is a helper state machine that negotiates the SSL connection automatically
 * without ever blocking any threads. It is important not to block here because
 * the TLS may occur in the selector thread which is one per all sockets.
 * 
 * Other than that the state machine is able to handle partial chunks of SIP messages
 * and only supply them when they are ready to the original TCP channel once they are
 * decrypted.
 */
public class SSLStateMachine {
	private static final Logger logger = Logger.getLogger(SSLStateMachine.class.getName());

	public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[] {});

	protected SSLEngine sslEngine;
	protected Queue<MessageSendItem> pendingOutboundBuffers = new LinkedList<>();
	protected NioTlsChannelInterface channel;
	protected ByteBuffer tlsRecordBuffer;
	private Object unwrapLock = new Object();
	private Object wrapLock = new Object();

	public SSLStateMachine(SSLEngine sslEngine, NioTlsChannelInterface channel) {
		this.sslEngine = sslEngine;
		this.channel = channel;
	}

	public void wrapRemaining() throws IOException {
		wrap(null, channel.prepareEncryptedDataBuffer(), null);
	}

	public void wrap(ByteBuffer src, ByteBuffer dst, MessageSendCallback callback) throws IOException {
		synchronized(wrapLock) {
			logger.log(Level.FINEST, "Wrapping {0}, buffers size {1}", new Object[] {
					src, pendingOutboundBuffers.size() });

			// Null source means we just have no ne data but still want to push any previously queued data
			if(src != null) {
				pendingOutboundBuffers.offer(new MessageSendItem(src, callback));
			}

			loop: while(true) {
				MessageSendItem currentBuffer = pendingOutboundBuffers.peek();

				// If there is no queued operations break out of the loop
				if(currentBuffer == null) break;

				SSLEngineResult result;
				try {
					result = sslEngine.wrap(currentBuffer.message, dst);

					logger.log(Level.FINEST, "Wrap result {0} buffers size {1}", new Object[] { result,
							pendingOutboundBuffers.size() });
				} finally {
					if(!currentBuffer.message.hasRemaining()) {
						pendingOutboundBuffers.remove();

						logger.log(Level.FINEST, "REMOVED item from encryption queue because it has no more data,"
								+ " all is done, buffers size now is {0} current buffer is {1}", new Object[] {
										pendingOutboundBuffers.size(), currentBuffer });
					}
				}

				int remaining = currentBuffer.message.remaining();

				logger.log(Level.FINEST, "Remaining {0} queue size is {1}", new Object[] {
						remaining, pendingOutboundBuffers.size()});

				if(0 < result.bytesProduced()) {
					// produced > 0 means encryption was successful and we have something to send over the wire
					dst.flip();

					byte[] bytes = new byte[dst.remaining()];

					dst.get(bytes);

					if(currentBuffer.getCallBack() != null) {
						// Send using message channel (it discriminates between client/server and new/old connections)
						currentBuffer.getCallBack().doSend(bytes);
					} else {
						// Send using the existing connection without attempting to guess client or server etc
						sendSSLMetadata(bytes);
					}

					dst.clear();
				} else {
					switch(result.getHandshakeStatus()) {
					case NEED_WRAP:
						if (currentBuffer.message.hasRemaining()) {
							break;
						} else {
							break loop;
						}
					case NEED_UNWRAP:
						break loop;
					case NEED_TASK:
						runDelegatedTasks(result);
						break;
					case FINISHED:
						// Added for https://java.net/jira/browse/JSIP-483 
						if(channel instanceof NioTlsMessageChannel) {
							((NioTlsMessageChannel)channel).setHandshakeCompleted(true);
							if(sslEngine.getSession() != null) {
								if(!ClientAuthType.Disabled.equals(channel.getSIPStack().getClientAuth())
										&& !ClientAuthType.DisabledAll.equals(channel.getSIPStack().getClientAuth())) {
									/*
									 * https://java.net/jira/browse/JSIP-483 Don't try to get the PeerCertificates
									 * if the client authentication is Disabled or DisabledAll
									 * as they won't be available
									 */
									try {
										((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
												.setPeerCertificates(sslEngine.getSession().getPeerCertificates());
									} catch(SSLPeerUnverifiedException e) {
										/*
										 * noop if -Dgov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled is used,
										 * no peer certificates will be available
										 */
										logger.log(Level.FINEST, "sslEngine.getSession().getPeerCertificates() are"
												+ " not available, which is normal if running with"
												+ " gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled");
									}
								}

								((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
										.setLocalCertificates(sslEngine.getSession().getLocalCertificates());
								((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
										.setCipherSuite(sslEngine.getSession().getCipherSuite());
							}
						}

						break;
					case NOT_HANDSHAKING:
						break loop;
					default:
						break;
					}
				}
			}
		}
	}

	private void wrapNonAppData() throws Exception {
		ByteBuffer encryptedDataBuffer = channel.prepareEncryptedDataBuffer();

		SSLEngineResult result;
		loop: while(true) {
			result = sslEngine.wrap(EMPTY_BUFFER, encryptedDataBuffer);

			logger.log(Level.FINEST, "NonAppWrap result {0} buffers size {1}", new Object[] { result,
					pendingOutboundBuffers.size() });

			if(result.bytesProduced() > 0) {
				// any output here is internal TLS metadata such as handshakes
				encryptedDataBuffer.flip();

				byte[] msg = new byte[encryptedDataBuffer.remaining()];

				encryptedDataBuffer.get(msg);

				// send it directly over the wire without further processing or parsing

				sendSSLMetadata(msg);

				encryptedDataBuffer.clear();
			}

			switch(result.getHandshakeStatus()) {
			case FINISHED:
				logger.log(Level.FINEST, "Handshake complete!");

				// Added for https://java.net/jira/browse/JSIP-483 
				if(channel instanceof NioTlsMessageChannel) {
					((NioTlsMessageChannel)channel).setHandshakeCompleted(true);
					if(sslEngine.getSession() != null) {
						if(!ClientAuthType.Disabled.equals(channel.getSIPStack().getClientAuth())
								&& !ClientAuthType.DisabledAll.equals(channel.getSIPStack().getClientAuth())) {
							/*
							 * https://java.net/jira/browse/JSIP-483 Don't try to get the PeerCertificates
							 * if the client authentication is Disabled or DisabledAll as they won't be available
							 */
							try {
								((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
										.setPeerCertificates(sslEngine.getSession().getPeerCertificates());
							} catch(SSLPeerUnverifiedException e) {
								/*
								 * noop if -Dgov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled is used,
								 * no peer certificates will be available
								 */

								logger.log(Level.FINEST, "sslEngine.getSession().getPeerCertificates()"
										+ " are not available, which is normal if running with"
										+ " gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled");
							}
						}

						((NioTlsMessageChannel) channel).getHandshakeCompletedListener()
								.setLocalCertificates(sslEngine.getSession().getLocalCertificates());
						((NioTlsMessageChannel) channel).getHandshakeCompletedListener()
								.setCipherSuite(sslEngine.getSession().getCipherSuite());
					}
				}

				break;
			case NEED_TASK:
				runDelegatedTasks(result);

				break;
			}

			if(0 == result.bytesProduced()) {
				break;
			}
		}
	}

	public void unwrap(ByteBuffer src) throws Exception {
		unwrap(src, channel.prepareAppDataBuffer());
	}

	private void startBuffer(ByteBuffer src) {
		if(tlsRecordBuffer == null) {

			// Begin buffering, if there is already a buffer the normalization will take of adding the bytes
			// max record size in other implementations
			tlsRecordBuffer = ByteBufferFactory.getInstance().allocateDirect(33270); 

			// Append the current buffer
			tlsRecordBuffer.put(src);

			// Prepare the buffer for reading
			tlsRecordBuffer.flip();

			logger.log(Level.FINEST, "Allocated record buffer for reading: {0} for src: {1}",
					new Object[] { tlsRecordBuffer, src });
		}
	}

	private void clearBuffer() {
		tlsRecordBuffer = null;

		logger.log(Level.FINEST, "Buffer cleared");
	}

	private ByteBuffer normalizeTlsRecordBuffer(ByteBuffer src) {
		if(tlsRecordBuffer == null) {
			return src;
		}

		logger.log(Level.FINEST, "Normalize buffer {0} into record buffer {1}", new Object[] { src, tlsRecordBuffer });

		// Reverse flip() to prepare the buffer to writing in append mode
		tlsRecordBuffer.position(tlsRecordBuffer.limit());
		tlsRecordBuffer.limit(tlsRecordBuffer.capacity());

		// Append data
		tlsRecordBuffer.put(src);

		// And prepare it for reading again as if it came from the network
		tlsRecordBuffer.flip();

		return tlsRecordBuffer;
	}

	private void unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		synchronized(unwrapLock) {
			loop: while(true) {
				src = normalizeTlsRecordBuffer(src);

				logger.log(Level.FINEST, "Unwrap src: {0}, dst: {1}", new Object[] { src, dst });

				SSLEngineResult result = null;

				/*
				 * https://java.net/jira/browse/JSIP-464 Make sure to throw the exception so the result variable
				 * is not null below which makes the stack hang
				 */
				result = sslEngine.unwrap(src, dst);

				logger.log(Level.FINEST, "Unwrap result {0}, buffers size: {1}, src: {2}, dst: {3}", new Object[] {
						result, pendingOutboundBuffers.size(), src, dst});

				if(result.getStatus().equals(Status.BUFFER_UNDERFLOW)) {
					logger.log(Level.FINEST, "Buffer underflow, wait for the next inbound chunk of data to feed"
							+ " the SSL engine");

					startBuffer(src);

					break;
				}

				clearBuffer();

				if(result.getStatus().equals(Status.BUFFER_OVERFLOW)) {
					logger.log(Level.FINEST, "Buffer overflow , must prepare the buffer again. outNetBuffer"
							+ " remaining: {0}, outNetBuffer postion: {1}, Packet buffer size: {2},"
							+ " new buffer size: {3}", new Object[] { dst.remaining(), dst.position(),
									sslEngine.getSession().getPacketBufferSize(), sslEngine.getSession()
											.getPacketBufferSize() + dst.position()});

					ByteBuffer newBuf = channel.prepareAppDataBuffer(sslEngine.getSession().getPacketBufferSize()
							+ dst.position());

					dst.flip();

					newBuf.put(dst);

					dst = newBuf;

					logger.log(Level.FINEST, "new outNetBuffer remaining: {0}, new outNetBuffer postion: {1}",
							new Object[] { dst.remaining(), dst.position() });

					continue;
				}

				if(result.bytesProduced()>0) {
					// There is actual application data in this chunk
					dst.flip();

					byte[] a = new byte[dst.remaining()];

					dst.get(a);

					// take it and feed the plain text to out chunk-by-chunk parser

					channel.addPlaintextBytes(a);
				}

				switch(result.getHandshakeStatus()) {
				case NEED_UNWRAP:
					logger.log(Level.FINEST, "Unwrap has remaining: {0} buffer {1}", new Object[] {
							src.hasRemaining(), src});

					if(src.hasRemaining()) {
						break;
					}

					break loop;
				case NEED_WRAP:
					wrapNonAppData();
					break;
				case NEED_TASK:
					runDelegatedTasks(result);
					break;
				case FINISHED:
					logger.log(Level.FINEST, "Handshaking just finnished, but has remaining. Will try to wrap"
							+ " the queues app items.");

					wrapRemaining();
					if(src.hasRemaining()) {
						break;
					}

					logger.log(Level.FINEST, "Handshake passed");

					/*
					 * Added for https://java.net/jira/browse/JSIP-483 allow application to enforce policy
					 * by validating the certificate
					 */
					if(channel instanceof NioTlsMessageChannel) {
						((NioTlsMessageChannel)channel).setHandshakeCompleted(true);
						if(sslEngine.getSession() != null) {
							if(!ClientAuthType.Disabled.equals(channel.getSIPStack().getClientAuth())
									&& !ClientAuthType.DisabledAll.equals(channel.getSIPStack().getClientAuth())) {
								/*
								 * https://java.net/jira/browse/JSIP-483 Don't try to get the PeerCertificates if
								 * the client authentication is Disabled or DisabledAll as they won't be available
								 */
								try {
									((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
											.setPeerCertificates(sslEngine.getSession().getPeerCertificates());
								} catch (SSLPeerUnverifiedException e) {
									/*
									 * noop if -Dgov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled is used, no peer
									 * certificates will be available
									 */
									logger.log(Level.FINEST, "sslEngine.getSession().getPeerCertificates() are not"
											+ " available, which is normal if running with"
											+ " gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled");
								}
							}

							((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
									.setLocalCertificates(sslEngine.getSession().getLocalCertificates());
							((NioTlsMessageChannel)channel).getHandshakeCompletedListener()
									.setCipherSuite(sslEngine.getSession().getCipherSuite());
						}

						try {
							channel.getSIPStack().getTlsSecurityPolicy()
									.enforceTlsPolicy(channel.getEncapsulatedClientTransaction());
						} catch(SecurityException ex) {
							throw new IOException(ex.getMessage());
						}

						logger.log(Level.FINEST, "TLS Security policy passed");
					}

					break loop;
				case NOT_HANDSHAKING:
					wrapRemaining();

					logger.log(Level.FINEST, "Not handshaking, but has remaining: {0}, buffer: {1}",
							new Object[] { src.hasRemaining(), src });

					if(src.hasRemaining()) {
						break;
					}

					break loop;
				default:
					break;
				}
			}
		}
	}

	private void runDelegatedTasks(SSLEngineResult result) throws IOException {
		logger.log(Level.FINEST, "Running delegated task for {0}", result);

		/*
		 *  Delegated tasks are just invisible steps inside the sslEngine state machine.
		 *  Call them every time they have NEED_TASK otherwise the sslEngine won't make progress
		 */
		if(result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			Runnable runnable;
			while((runnable = sslEngine.getDelegatedTask()) != null) {
				runnable.run();
			}

			HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();

			logger.log(Level.FINEST, "Handshake status after delegated tasks {0}", hsStatus);

			if(HandshakeStatus.NEED_TASK == hsStatus) {
				throw new IOException("handshake shouldn't need additional tasks");
			}
		}
	}

	public void sendSSLMetadata(byte[] msg) throws IOException {
		channel.sendEncryptedData(msg);
	}

	public static interface MessageSendCallback {
		public void doSend(byte[] bytes) throws IOException;
	}

	/**
	 * Each time we send a SIP message it will be associated with the originating channel.send() method
	 * which keep additional information about the transport in its context. That method will be called
	 * using the callback provided here.
	 */
	public static class MessageSendItem {
		private ByteBuffer message;
		private MessageSendCallback callback;

		public MessageSendItem(ByteBuffer buffer, MessageSendCallback callback) {
			this.message = buffer;
			this.callback = callback;
		}

		public MessageSendCallback getCallBack() {
			return callback;
		}

		@Override
		public String toString() {
			return MessageSendItem.class.getSimpleName() + " [" + message + ", " + callback + "]";
		}
	}
}
