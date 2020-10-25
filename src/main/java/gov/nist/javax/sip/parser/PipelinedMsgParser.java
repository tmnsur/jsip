package gov.nist.javax.sip.parser;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.stack.ConnectionOrientedMessageChannel;
import gov.nist.javax.sip.stack.QueuedMessageDispatchBase;
import gov.nist.javax.sip.stack.SIPTransactionStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implements a pipelined message parser suitable for use with a stream -
 * oriented input such as TCP. The client uses this class by instatiating with
 * an input stream from which input is read and fed to a message parser. It
 * keeps reading from the input stream and process messages in a never ending
 * interpreter loop. The message listener interface gets called for processing
 * messages or for processing errors. The payload specified by the
 * content-length header is read directly from the input stream. This can be
 * accessed from the SIPMessage using the getContent and getContentBytes methods
 * provided by the SIPMessage class.
 *
 * the parser was blocking so I threw out some cool pipelining which ran fast but only worked
 * when the phase of the moon matched its mood. Now things are serialized and
 * life goes slower but more reliably.
 * 
 * @see SIPMessageListener
 */
public final class PipelinedMsgParser implements Runnable {
	private static final Logger logger = Logger.getLogger(PipelinedMsgParser.class.getName());

	private static final String CRLF = "\r\n";

	/**
	 * The message listener that is registered with this parser. (The message
	 * listener has methods that can process correct and erroneous messages.)
	 */
	protected SIPMessageListener sipMessageListener;

	// Preprocessor thread
	private Thread mythread;
	private Pipeline rawInputStream;
	private int maxMessageSize;
	private int sizeCounter;
	private SIPTransactionStack sipStack;
	private MessageParser smp = null;
	private ConcurrentHashMap<String, CallIDOrderingStructure> messagesOrderingMap = new ConcurrentHashMap<>();
	boolean isRunning = false;

	/**
	 * default constructor.
	 */
	protected PipelinedMsgParser() {
		super();
	}

	private static int uid = 0;

	private static synchronized int getNewUid() {
		return uid++;
	}

	/**
	 * Constructor when we are given a message listener and an input stream (could
	 * be a TCP connection or a file)
	 *
	 * @param sipMessageListener Message listener which has methods that get called
	 *                           back from the parser when a parse is complete
	 * @param in                 Input stream from which to read the input.
	 * @param debug              Enable/disable tracing or lexical analyser switch.
	 */
	public PipelinedMsgParser(SIPTransactionStack sipStack, SIPMessageListener sipMessageListener, Pipeline in,
			boolean debug, int maxMessageSize) {
		this();
		this.sipStack = sipStack;
		smp = sipStack.getMessageParserFactory().createMessageParser(sipStack);
		this.sipMessageListener = sipMessageListener;
		rawInputStream = in;
		this.maxMessageSize = maxMessageSize;
		mythread = new Thread(this);
		mythread.setName("PipelineThread-" + getNewUid());

	}

	/**
	 * This is the constructor for the pipelined parser.
	 *
	 * @param mhandler a SIPMessageListener implementation that provides the message
	 *                 handlers to handle correctly and incorrectly parsed messages.
	 * @param in       An input stream to read messages from.
	 */

	public PipelinedMsgParser(SIPTransactionStack sipStack, SIPMessageListener mhandler, Pipeline in, int maxMsgSize) {
		this(sipStack, mhandler, in, false, maxMsgSize);
	}

	/**
	 * This is the constructor for the pipelined parser.
	 *
	 * @param in - An input stream to read messages from.
	 */

	public PipelinedMsgParser(SIPTransactionStack sipStack, Pipeline in) {
		this(sipStack, null, in, false, 0);
	}

	/**
	 * Start reading and processing input.
	 */
	public void processInput() {
		mythread.start();
	}

	/**
	 * Create a new pipelined parser from an existing one.
	 *
	 * @return A new pipelined parser that reads from the same input stream.
	 */
	@Override
	protected Object clone() {
		PipelinedMsgParser p = new PipelinedMsgParser();

		p.rawInputStream = this.rawInputStream;
		p.sipMessageListener = this.sipMessageListener;

		Thread mythread = new Thread(p);

		mythread.setName("PipelineThread");

		return p;
	}

	/**
	 * Add a class that implements a SIPMessageListener interface whose methods get
	 * called * on successful parse and error conditions.
	 *
	 * @param mlistener a SIPMessageListener implementation that can react to
	 *                  correct and incorrect pars.
	 */

	public void setMessageListener(SIPMessageListener mlistener) {
		sipMessageListener = mlistener;
	}

	/**
	 * read a line of input. Note that we encode the result in UTF-8
	 */

	private String readLine(InputStream inputStream) throws IOException {
		int counter = 0;
		int increment = 1024;
		int bufferSize = increment;
		byte[] lineBuffer = new byte[bufferSize];
		// handles RFC 5626 CRLF keep-alive mechanism
		byte[] crlfBuffer = new byte[2];
		int crlfCounter = 0;
		while (true) {
			char ch;
			int i = inputStream.read();
			if (i == -1) {
				throw new IOException("End of stream");
			} else
				ch = (char) (i & 0xFF);
			// reduce the available read size by 1 ("size" of a char).
			if (this.maxMessageSize > 0) {
				this.sizeCounter--;
				if (this.sizeCounter <= 0)
					throw new IOException("Max size exceeded!");
			}
			if (ch != '\r')
				lineBuffer[counter++] = (byte) (i & 0xFF);
			else if (counter == 0)
				crlfBuffer[crlfCounter++] = (byte) '\r';

			if (ch == '\n') {
				if (counter == 1 && crlfCounter > 0) {
					crlfBuffer[crlfCounter++] = (byte) '\n';
				}
				break;
			}

			if (counter == bufferSize) {
				byte[] tempBuffer = new byte[bufferSize + increment];
				System.arraycopy((Object) lineBuffer, 0, (Object) tempBuffer, 0, bufferSize);
				bufferSize = bufferSize + increment;
				lineBuffer = tempBuffer;

			}
		}

		if(counter == 1 && crlfCounter > 0) {
			return new String(crlfBuffer, 0, crlfCounter, StandardCharsets.UTF_8);
		}

		return new String(lineBuffer, 0, counter, StandardCharsets.UTF_8);
	}

	public class Dispatch implements Runnable, QueuedMessageDispatchBase {
		CallIDOrderingStructure callIDOrderingStructure;
		String callId;
		long time;

		public Dispatch(CallIDOrderingStructure callIDOrderingStructure, String callId) {
			this.callIDOrderingStructure = callIDOrderingStructure;
			this.callId = callId;
			time = System.currentTimeMillis();
		}

		public void run() {
			// we acquire it in the thread to avoid blocking other messages with a different
			// call id
			// that could be processed in parallel
			Semaphore semaphore = callIDOrderingStructure.getSemaphore();
			final Queue<SIPMessage> messagesForCallID = callIDOrderingStructure.getMessagesForCallID();
			if (sipStack.sipEventInterceptor != null) {
				sipStack.sipEventInterceptor.beforeMessage(messagesForCallID.peek());
			}
			try {
				semaphore.acquire();
			} catch(InterruptedException e) {
				logger.log(Level.SEVERE, e,
						() -> MessageFormat.format("Semaphore acquisition for callId {0} interrupted", callId));
			}

			// once acquired we get the first message to process
			SIPMessage message = messagesForCallID.poll();

			logger.log(Level.FINEST, "semaphore acquired for message: {0}", message);

			try {
				sipMessageListener.processMessage(message);
			} catch(Exception e) {
				logger.log(Level.SEVERE, "Error occured processing message", e);

				// We do not break the TCP connection because other calls use the same socket here
			} finally {
				if(messagesForCallID.isEmpty()) {
					messagesOrderingMap.remove(callId);

					logger.log(Level.FINEST, "CallIDOrderingStructure removed for message: {0}", callId);
				}

				logger.log(Level.FINEST, "releasing semaphore for message: {0}", message);

				/**
				 * release the semaphore so that another thread can process another message from the call id queue
				 * in the correct order or a new message from another call id queue
				 */
				semaphore.release();

				if(messagesOrderingMap.isEmpty()) {
					synchronized (messagesOrderingMap) {
						messagesOrderingMap.notify();
					}
				}

				if(sipStack.sipEventInterceptor != null) {
					sipStack.sipEventInterceptor.afterMessage(message);
				}
			}

			logger.log(Level.FINEST, "dispatch task done on: {0} threadname: {1}",
					new Object[] {message, mythread.getName()});
		}

		public long getReceptionTime() {
			return time;
		}
	}

	/**
	 * This is input reading thread for the pipelined parser. You feed it input
	 * through the input stream (see the constructor) and it calls back an event
	 * listener interface for message processing or error. It cleans up the input -
	 * dealing with things like line continuation
	 */
	@Override
	public void run() {
		Pipeline inputStream = this.rawInputStream;

		// I cannot use buffered reader here because we may need to switch
		// encodings to read the message body.
		try {
			isRunning = true;
			while (isRunning) {
				this.sizeCounter = this.maxMessageSize;

				StringBuilder inputBuffer = new StringBuilder();

				logger.log(Level.FINEST, "Starting to parse.");

				String line1;
				String line2 = null;
				boolean isPreviousLineCRLF = false;
				while (true) {
					try {
						line1 = readLine(inputStream);
						// ignore blank lines.
						if(line1.equals("\n")) {
							logger.log(Level.FINEST, "Discarding blank line");

							continue;
						}

						if(CRLF.equals(line1)) {
							if (isPreviousLineCRLF) {
								// Handling keep-alive ping (double CRLF) as defined per RFC 5626 Section 4.4.1
								// sending pong (single CRLF)
								logger.log(Level.FINEST, "KeepAlive Double CRLF received, sending single CRLF"
										+ " as defined per RFC 5626 Section 4.4.1");
								logger.log(Level.FINEST, "~~~ setting isPreviousLineCRLF=false");

								isPreviousLineCRLF = false;

								try {
									sipMessageListener.sendSingleCRLF();
								} catch(Exception e) {
									logger.log(Level.FINEST, "A problem occured while trying to send a single CRLF"
											+ " in response to a double CRLF", e);
								}

								continue;
							} else {
								isPreviousLineCRLF = true;

								logger.log(Level.FINEST, "Received CRLF");

								if(sipMessageListener != null
										&& sipMessageListener instanceof ConnectionOrientedMessageChannel) {
									((ConnectionOrientedMessageChannel) sipMessageListener)
											.cancelPingKeepAliveTimeoutTaskIfStarted();
								}
							}

							continue;
						} else {
							break;
						}
					} catch(IOException ex) {
						/*
						 * we only wait if the thread is still in a running state and hasn't been close
						 * from somewhere else or we are leaking because the thread is waiting forever
						 */
						if(PostParseExecutorServices.getPostParseExecutor() != null && isRunning) {
							logger.log(Level.FINEST, "waiting for messagesOrderingMap {0} threadname {1}",
									new Object[] { this, mythread.getName() });

							synchronized (messagesOrderingMap) {
								try {
									messagesOrderingMap.wait(64000);
								} catch (InterruptedException e) {
								}
							}

							logger.log(Level.FINEST, "got notified for messagesOrderingMap {0} threadname {1}",
									new Object[] {this, mythread.getName()});
						}

						this.rawInputStream.stopTimer();

						logger.log(Level.FINEST, "thread ending for threadname {0}", mythread.getName());

						return;
					}
				}

				inputBuffer.append(line1);
				// Guard against bad guys.
				this.rawInputStream.startTimer();
				int bytesRead = 0;

				logger.log(Level.FINEST, "Reading Input stream.");

				while(true) {
					try {
						line2 = readLine(inputStream);
						bytesRead += line2.length();
						if (maxMessageSize > 0 && bytesRead > (maxMessageSize / 2))
							throw new IOException(
									"Pre-content-length headers size exceeded. The size of the message of the headers prior to Content-Length is too large. This must be an invalid message. Limit is MAX_MESSAGE_SIZE/2="
											+ maxMessageSize / 2);
						inputBuffer.append(line2);
						if(line2.trim().equals("")) {
							break;
						}
					} catch(IOException ex) {
						// we only wait if the thread is still in a running state and hasn't been close
						// from somewhere else
						// or we are leaking because the thread is waiting forever
						if(PostParseExecutorServices.getPostParseExecutor() != null && isRunning) {
							logger.log(Level.FINEST, "waiting for messagesOrderingMap {0} threadname {1}",
									new Object[] {this, mythread.getName()});

							synchronized(messagesOrderingMap) {
								try {
									messagesOrderingMap.wait(64000);
								} catch (InterruptedException e) {
								}
							}

							logger.log(Level.FINEST, "got notified for messagesOrderingMap {0} threadname {1}",
									new Object[] {this, mythread.getName()});
						}

						this.rawInputStream.stopTimer();

						logger.log(Level.FINEST, "thread ending for threadname {0}", mythread.getName());

						return;
					}
				}

				// Stop the timer that will kill the read.
				this.rawInputStream.stopTimer();

				inputBuffer.append(line2);

				SIPMessage sipMessage = null;

				try {
					logger.log(Level.FINEST, "About to parse: {0}", inputBuffer);

					byte[] inputBufferBytes = null;
					try {
						inputBufferBytes = inputBuffer.toString().getBytes("UTF-8");
					} catch (UnsupportedEncodingException e) {
						// logging required ??
					}
					if (inputBufferBytes != null) {
						sipMessage = smp.parseSIPMessage(inputBufferBytes, false, false, sipMessageListener);
					} else {
						sipMessage = smp.parseSIPMessage(inputBuffer.toString().getBytes(), false, false,
								sipMessageListener);
					}
					if (sipMessage == null) {
						this.rawInputStream.stopTimer();
						continue;
					}
				} catch(ParseException ex) {
					// Just ignore the parse exception.
					logger.log(Level.SEVERE, "Detected a parse error", ex);

					continue;
				}

				logger.log(Level.FINEST, "Completed parsing message");

				String clString = sipMessage.getHeaderAsFormattedString(ContentLength.NAME);

				if(clString.length() > 30) {
					throw new IllegalStateException("Bad content lenght header " + clString);
				}

				ContentLength cl = (ContentLength) sipMessage.getContentLength();

				int contentLength = 0;
				if (cl != null) {
					contentLength = cl.getContentLength();
				} else {
					contentLength = 0;
				}

				logger.log(Level.FINEST, "Content length = {0}", contentLength);

				if(maxMessageSize > 0 && contentLength > maxMessageSize) {
					throw new IllegalStateException("Max content size Exceeded! :" + contentLength
							+ " allowed max size is " + maxMessageSize);
				}

				if(contentLength == 0) {
					sipMessage.removeContent();
				} else if(maxMessageSize == 0 || contentLength < this.sizeCounter) {
					byte[] message_body = new byte[contentLength];

					int nread = 0;
					while(nread < contentLength) {
						/* Start my starvation timer. This ensures that the other end writes at least some data in
						 * or we will close the pipe from him. This prevents DOS attack that takes up
						 * all our connections.
						 */
						this.rawInputStream.startTimer();

						try {
							int readlength = inputStream.read(message_body, nread, contentLength - nread);
							if (readlength > 0) {
								nread += readlength;
							} else {
								break;
							}
						} catch (IOException ex) {
							logger.log(Level.SEVERE, "Exception Reading Content", ex);

							break;
						} finally {
							this.rawInputStream.stopTimer();
						}
					}

					sipMessage.setMessageContent(message_body);
				}

				// Content length too large - process the message and return error from there.
				if(sipMessageListener != null) {
					try {
						if(PostParseExecutorServices.getPostParseExecutor() == null) {
							/**
							 * If gov.nist.javax.sip.TCP_POST_PARSING_THREAD_POOL_SIZE is disabled we
							 * continue with the old logic here.
							 */
							if(sipStack.sipEventInterceptor != null) {
								sipStack.sipEventInterceptor.beforeMessage(sipMessage);
							}

							sipMessageListener.processMessage(sipMessage);

							if(sipStack.sipEventInterceptor != null) {
								sipStack.sipEventInterceptor.afterMessage(sipMessage);
							}
						} else {
							/**
							 * gov.nist.javax.sip.TCP_POST_PARSING_THREAD_POOL_SIZE is enabled so we use the
							 * thread pool to execute the task.
							 */
							// we need to guarantee message ordering on the same socket on TCP
							// so we lock and queue of messages per Call Id

							final String callId = sipMessage.getCallId().getCallId();
							// http://dmy999.com/article/34/correct-use-of-concurrenthashmap
							CallIDOrderingStructure orderingStructure = messagesOrderingMap.get(callId);
							if (orderingStructure == null) {
								CallIDOrderingStructure newCallIDOrderingStructure = new CallIDOrderingStructure();
								orderingStructure = messagesOrderingMap.putIfAbsent(callId, newCallIDOrderingStructure);
								if(orderingStructure == null) {
									orderingStructure = newCallIDOrderingStructure;

									logger.log(Level.FINEST, "new CallIDOrderingStructure added for message {0}",
											sipMessage);
								}
							}
							final CallIDOrderingStructure callIDOrderingStructure = orderingStructure;
							// we add the message to the pending queue of messages to be processed for that
							// call id here
							// to avoid blocking other messages with a different call id
							// that could be processed in parallel
							callIDOrderingStructure.getMessagesForCallID().offer(sipMessage);

							PostParseExecutorServices.getPostParseExecutor()
									.execute(new Dispatch(callIDOrderingStructure, callId)); // run in executor thread
						}
					} catch (Exception ex) {
						// fatal error in processing - close the
						// connection.
						break;
					}
				}
			}
		} finally {
			try {
				cleanMessageOrderingMap();
				if (!inputStream.isClosed()) {
					inputStream.close();
				}
			} catch (IOException e) {
				InternalErrorHandler.handleException(e);
			}
		}
	}

	/**
	 * Data structure to make sure ordering of Messages is guaranteed under TCP when
	 * the post parsing thread pool is used
	 * 
	 * @author jean.deruelle@gmail.com
	 *
	 */
	class CallIDOrderingStructure {
		private Semaphore semaphore;
		private Queue<SIPMessage> messagesForCallID;

		public CallIDOrderingStructure() {
			semaphore = new Semaphore(1, true);
			messagesForCallID = new ConcurrentLinkedQueue<SIPMessage>();
		}

		/**
		 * @return the semaphore
		 */
		public Semaphore getSemaphore() {
			return semaphore;
		}

		/**
		 * @return the messagesForCallID
		 */
		public Queue<SIPMessage> getMessagesForCallID() {
			return messagesForCallID;
		}
	}

	public void close() {
		isRunning = false;

		logger.log(Level.FINEST, "Closing pipelinedmsgparser {0} threadname {1}",
				new Object[] {this, mythread.getName()});

		try {
			this.rawInputStream.close();
		} catch (IOException ex) {
			logger.log(Level.FINEST, "Couldn't close the rawInputStream {0} threadname {1} already closed ? {2}",
					new Object[] {this, mythread.getName(), rawInputStream.isClosed()});
		}

		if(PostParseExecutorServices.getPostParseExecutor() != null) {
			cleanMessageOrderingMap();

			synchronized(mythread) {
				mythread.notifyAll();
				// interrupting because there is a race condition on the
				// messagesOrderingMap.wait() that
				// eventually leads to thread leaking and OutOfMemory
				mythread.interrupt();
			}
		}
	}

	private void cleanMessageOrderingMap() {
		messagesOrderingMap.clear();

		synchronized (messagesOrderingMap) {
			messagesOrderingMap.notifyAll();
		}

		logger.log(Level.FINEST, "cleaned the messagesOrderingMap {0}, threadname {1}",
				new Object[] {this, mythread.getName()});
	}
}
