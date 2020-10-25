package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransactionImpl.ExpiresTimerTask;

import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.sip.IOExceptionEvent;
import javax.sip.TransactionState;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Abstract class to support both client and server transactions. Provides an
 * encapsulation of a message channel, handles timer events, and creation of the
 * Via header for a message.
 */
public abstract class SIPTransactionImpl implements SIPTransaction {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(SIPTransactionImpl.class.getName());

	// Contribution on http://java.net/jira/browse/JSIP-417
	private static final Pattern EXTRACT_CN = Pattern.compile(".*CN\\s*=\\s*([\\w*\\.\\-_]+).*");

	private static final String NOT_A_TLS_CHANNEL = "Not a TLS channel";

	// Flag to indicate that the listener gets
	protected boolean toListener;

	// to see the event.
	protected int baseTimerInterval = SIPTransactionStack.BASE_TIMER_INTERVAL;

	/**
	 * 5s Maximum duration a message will remain in the network
	 */
	protected int t4 = 5000 / baseTimerInterval;

	/**
	 * The maximum retransmit interval for non-INVITE requests and INVITE responses
	 */
	protected int t2 = 4000 / baseTimerInterval;

	protected int timerI = t4;
	protected int timerK = t4;
	protected int timerD = 32000 / baseTimerInterval;

	// Proposed feature for next release.
	protected transient Object applicationData;

	protected SIPResponse lastResponse;
	protected boolean isMapped;
	private transient TransactionSemaphore semaphore;

	// Transaction Id.
	protected String transactionId;

	// Audit tag used by the SIP Stack audit
	protected long auditTag = 0;

	// Parent stack for this transaction
	protected transient SIPTransactionStack sipStack;

	// Original request that is being handled by this transaction
	protected SIPRequest originalRequest;

	// we nullify the originalRequest fast to save on mem and help GC so we keep only those data instead
	protected byte[] originalRequestBytes;
	protected long originalRequestCSeqNumber;
	protected String originalRequestBranch;
	protected boolean originalRequestHasPort;

	// Underlying channel being used to send messages for this transaction
	protected transient MessageChannel encapsulatedChannel;

	protected AtomicBoolean transactionTimerStarted = new AtomicBoolean(false);

	// Transaction branch ID
	private String branch;

	// Method of the Request used to create the transaction.
	private String method;

	// Current transaction state
	private int currentState = -1;

	// Number of ticks the retransmission timer was set to last
	private transient int retransmissionTimerLastTickCount;

	// Number of ticks before the message is retransmitted
	private transient int retransmissionTimerTicksLeft;

	// Number of ticks before the transaction times out
	protected int timeoutTimerTicksLeft;

	// List of event listeners for this transaction
	private transient Set<SIPTransactionEventListener> eventListeners;

	// Counter for caching of connections.
	// Connection lingers for collectionTime
	// after the Transaction goes to terminated state.
	protected int collectionTime;

	private boolean terminatedEventDelivered;

	// aggressive flag to optimize eagerly
	private boolean releaseReferences;

	// caching flags
	private Boolean inviteTransaction = null;
	private Boolean dialogCreatingTransaction = null;

	// caching fork id
	private String forkId = null;
	protected String mergeId = null;

	public ExpiresTimerTask expiresTimerTask;

	// http://java.net/jira/browse/JSIP-420
	private MaxTxLifeTimeListener maxTxLifeTimeListener;

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getBranchId()
	 */
	@Override
	public String getBranchId() {
		return this.branch;
	}

	// [Issue 284] https://jain-sip.dev.java.net/issues/show_bug.cgi?id=284
	// JAIN SIP drops 200 OK due to race condition
	// Wrapper that uses a semaphore for non reentrant listener
	// and a lock for reentrant listener to avoid race conditions
	// when 2 responses 180/200 OK arrives at the same time
	class TransactionSemaphore {
		Semaphore sem = null;
		ReentrantLock lock = null;

		public TransactionSemaphore() {
			if(((SipStackImpl) sipStack).isReEntrantListener()) {
				lock = new ReentrantLock();
			} else {
				sem = new Semaphore(1, true);
			}
		}

		public boolean acquire() {
			try {
				if(((SipStackImpl) sipStack).isReEntrantListener()) {
					lock.lock();
				} else {
					sem.acquire();
				}

				return true;
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Unexpected exception acquiring sem", ex);

				InternalErrorHandler.handleException(ex);

				return false;
			}
		}

		public boolean tryAcquire() {
			try {
				if(((SipStackImpl) sipStack).isReEntrantListener()) {
					return lock.tryLock(sipStack.maxListenerResponseTime, TimeUnit.SECONDS);
				}

				return sem.tryAcquire(sipStack.maxListenerResponseTime, TimeUnit.SECONDS);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Unexpected exception trying acquiring sem", ex);

				InternalErrorHandler.handleException(ex);

				return false;
			}
		}

		public void release() {
			try {
				if(((SipStackImpl) sipStack).isReEntrantListener()) {
					if(lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				} else {
					sem.release();
				}
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Unexpected exception releasing sem", ex);
			}
		}
	}

	/**
	 * The linger timer is used to remove the transaction from the transaction table
	 * after it goes into terminated state. This allows connection caching and also
	 * takes care of race conditions.
	 */
	class LingerTimer extends SIPStackTimerTask {
		public LingerTimer() {
			SIPTransaction sipTransaction = SIPTransactionImpl.this;

			logger.log(Level.FINEST, "LingerTimer: {0}", sipTransaction.getTransactionId());
		}

		@Override
		public void runTask() {
			cleanUp();
		}
	}

	/**
	 * http://java.net/jira/browse/JSIP-420 This timer task will terminate the transaction after a configurable time
	 */
	class MaxTxLifeTimeListener extends SIPStackTimerTask {
		SIPTransaction sipTransaction = SIPTransactionImpl.this;

		@Override
		public void runTask() {
			try {
				logger.log(Level.FINEST, "Fired MaxTxLifeTimeListener for TX {0}, TX id: {1},state: {2}",
						new Object[] { sipTransaction, sipTransaction.getTransactionId(), sipTransaction.getState() });

				raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);

				SIPStackTimerTask myTimer = new LingerTimer();

				sipStack.getTimer().schedule(myTimer, SIPTransactionStack.CONNECTION_LINGER_TIME * 1000L);

				maxTxLifeTimeListener = null;
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "unexpected exception", ex);
			}
		}
	}

	/**
	 * Transaction constructor.
	 *
	 * @param newParentStack         Parent stack for this transaction.
	 * @param newEncapsulatedChannel Underlying channel for this transaction.
	 */
	protected SIPTransactionImpl(SIPTransactionStack newParentStack, MessageChannel newEncapsulatedChannel) {
		sipStack = newParentStack;

		this.semaphore = new TransactionSemaphore();

		encapsulatedChannel = newEncapsulatedChannel;

		if(this.isReliable()) {
			encapsulatedChannel.useCount++;

			logger.log(Level.FINEST, "use count for encapsulated channel: {0} {1}", new Object[] { this,
					encapsulatedChannel.useCount });
		}

		this.currentState = -1;

		disableRetransmissionTimer();
		disableTimeoutTimer();

		eventListeners = new CopyOnWriteArraySet<>();

		// Always add the parent stack as a listener of this transaction
		addEventListener(newParentStack);

		releaseReferences = sipStack.isAggressiveCleanup();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setOriginalRequest(gov.nist.javax.sip.message.SIPRequest)
	 */
	@Override
	public void setOriginalRequest(SIPRequest newOriginalRequest) {
		// Branch value of topmost Via header
		String newBranch;

		String newTransactionId = newOriginalRequest.getTransactionId();
		if(this.originalRequest != null && (!this.originalRequest.getTransactionId().equals(newTransactionId))) {
			sipStack.removeTransactionHash(this);
		}

		// This will be cleared later.
		this.originalRequest = newOriginalRequest;
		this.originalRequestCSeqNumber = newOriginalRequest.getCSeq().getSeqNumber();

		Via topmostVia = newOriginalRequest.getTopmostVia();

		this.originalRequestBranch = topmostVia.getBranch();
		this.originalRequestHasPort = topmostVia.hasPort();

		// just cache the control information so the
		// original request can be released later.
		this.method = newOriginalRequest.getMethod();

		this.transactionId = newTransactionId;

		originalRequest.setTransaction(this);

		// If the message has an explicit branch value set,
		newBranch = topmostVia.getBranch();

		if(newBranch != null) {
			logger.log(Level.FINEST, "Setting Branch id: {0}", newBranch);

			// Override the default branch with the one
			// set by the message
			setBranch(newBranch);
		} else {
			logger.log(Level.FINEST, "Branch id is null - compute TID! {0}", newOriginalRequest.encode());

			setBranch(newTransactionId);
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getOriginalRequest()
	 */
	@Override
	public SIPRequest getOriginalRequest() {
		return this.originalRequest;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getRequest()
	 */
	@Override
	public Request getRequest() {
		if(isReleaseReferences() && originalRequest == null && originalRequestBytes != null) {
			logger.log(Level.WARNING, "reparsing original request: {0} since it was eagerly cleaned up, but beware this"
					+ " is not efficient with the aggressive flag set !", new Object[] { originalRequestBytes });

			try {
				originalRequest = (SIPRequest) sipStack.getMessageParserFactory().createMessageParser(sipStack)
						.parseSIPMessage(originalRequestBytes, true, false, null);
			} catch (ParseException e) {
				logger.log(Level.SEVERE, "message {0} could not be reparsed!", originalRequestBytes);
			}
		}

		return originalRequest;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isDialogCreatingTransaction()
	 */
	@Override
	public boolean isDialogCreatingTransaction() {
		if(dialogCreatingTransaction == null) {
			dialogCreatingTransaction = Boolean.valueOf(isInviteTransaction() || getMethod().equals(Request.SUBSCRIBE)
					|| getMethod().equals(Request.REFER));
		}

		return dialogCreatingTransaction.booleanValue();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isInviteTransaction()
	 */
	@Override
	public boolean isInviteTransaction() {
		if(inviteTransaction == null) {
			inviteTransaction = Boolean.valueOf(getMethod().equals(Request.INVITE));
		}

		return inviteTransaction.booleanValue();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isCancelTransaction()
	 */
	@Override
	public boolean isCancelTransaction() {
		return getMethod().equals(Request.CANCEL);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isByeTransaction()
	 */
	@Override
	public boolean isByeTransaction() {
		return getMethod().equals(Request.BYE);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getMessageChannel()
	 */
	@Override
	public MessageChannel getMessageChannel() {
		return encapsulatedChannel;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setBranch(java.lang.String)
	 */
	@Override
	public void setBranch(String newBranch) {
		branch = newBranch;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getBranch()
	 */
	@Override
	public String getBranch() {
		if(this.branch == null) {
			this.branch = originalRequestBranch;
		}

		return branch;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getMethod()
	 */
	@Override
	public String getMethod() {
		return this.method;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getCSeq()
	 */
	@Override
	public long getCSeq() {
		return this.originalRequestCSeqNumber;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setState(int)
	 */
	@Override
	public void setState(int newState) {
		if(currentState == TransactionState.COMPLETED_VALUE && newState != TransactionState.TERMINATED_VALUE
				&& newState != TransactionState.CONFIRMED_VALUE) {
			newState = TransactionState.COMPLETED_VALUE;
		}

		if(currentState == TransactionState.CONFIRMED_VALUE && newState != TransactionState.TERMINATED_VALUE) {
			newState = TransactionState.CONFIRMED_VALUE;
		}

		if(currentState != TransactionState.TERMINATED_VALUE) {
			currentState = newState;
		} else {
			newState = currentState;
		}

		if(newState == TransactionState.COMPLETED_VALUE) {
			// timer H must be started around now
			enableTimeoutTimer(TIMER_H);
		}

		logger.log(Level.FINEST, "Transaction:setState {0} {1} branchID: {2}, isClient: {3}", new Object[] { newState,
				this, getBranch(), this instanceof SIPClientTransaction });
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getInternalState()
	 */
	@Override
	public int getInternalState() {
		return this.currentState;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getState()
	 */
	@Override
	public TransactionState getState() {
		if(currentState < 0) {
			return null;
		}

		return TransactionState.getObject(this.currentState);
	}

	/**
	 * Enables retransmission timer events for this transaction to begin in one
	 * tick.
	 */
	protected void enableRetransmissionTimer() {
		enableRetransmissionTimer(1);
	}

	/**
	 * Enables retransmission timer events for this transaction to begin after the
	 * number of ticks passed to this routine.
	 *
	 * @param tickCount Number of ticks before the next retransmission timer event
	 *                  occurs.
	 */
	protected void enableRetransmissionTimer(int tickCount) {
		// For INVITE Client transactions, double interval each time
		if(isInviteTransaction() && (this instanceof SIPClientTransaction)) {
			retransmissionTimerTicksLeft = tickCount;
		} else {
			// non-INVITE transactions and 3xx-6xx responses are capped at T2
			retransmissionTimerTicksLeft = Math.min(tickCount, getTimerT2());
		}

		retransmissionTimerLastTickCount = retransmissionTimerTicksLeft;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#disableRetransmissionTimer()
	 */
	@Override
	public void disableRetransmissionTimer() {
		retransmissionTimerTicksLeft = -1;
	}

	/**
	 * Enables a timeout event to occur for this transaction after the number of
	 * ticks passed to this method.
	 *
	 * @param tickCount Number of ticks before this transaction times out.
	 */
	protected void enableTimeoutTimer(int tickCount) {
		logger.log(Level.FINEST, "enableTimeoutTimer: {0}, tickCount: {1}, currentTickCount: {2}", new Object[] {
				this, tickCount, timeoutTimerTicksLeft });

		timeoutTimerTicksLeft = tickCount;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#disableTimeoutTimer()
	 */
	@Override
	public void disableTimeoutTimer() {
		logger.log(Level.FINEST, "disableTimeoutTimer: {0}", this);

		timeoutTimerTicksLeft = -1;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#fireTimer()
	 */
	@Override
	public void fireTimer() {
		// If the timeout timer is enabled,

		// Count down the timer, and if it has run out,
		if(timeoutTimerTicksLeft != -1 && --timeoutTimerTicksLeft == 0) {
			fireTimeoutTimer();
		}

		// If the retransmission timer is enabled, Count down the timer, and if it has run out
		if(retransmissionTimerTicksLeft != -1 && --retransmissionTimerTicksLeft == 0) {
			// Enable this timer to fire again after twice the original time
			enableRetransmissionTimer(retransmissionTimerLastTickCount * 2);

			// Fire the timeout timer
			fireRetransmissionTimer();
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		return currentState == TransactionState.TERMINATED_VALUE;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getHost()
	 */
	@Override
	public String getHost() {
		return encapsulatedChannel.getHost();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getKey()
	 */
	@Override
	public String getKey() {
		return encapsulatedChannel.getKey();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPort()
	 */
	@Override
	public int getPort() {
		return encapsulatedChannel.getPort();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getSIPStack()
	 */
	@Override
	public SIPTransactionStack getSIPStack() {
		return sipStack;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPeerAddress()
	 */
	@Override
	public String getPeerAddress() {
		return this.encapsulatedChannel.getPeerAddress();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPeerPort()
	 */
	@Override
	public int getPeerPort() {
		return this.encapsulatedChannel.getPeerPort();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPeerPacketSourcePort()
	 */
	@Override
	public int getPeerPacketSourcePort() {
		return this.encapsulatedChannel.getPeerPacketSourcePort();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPeerPacketSourceAddress()
	 */
	@Override
	public InetAddress getPeerPacketSourceAddress() {
		return this.encapsulatedChannel.getPeerPacketSourceAddress();
	}

	public InetAddress getPeerInetAddress() {
		return this.encapsulatedChannel.getPeerInetAddress();
	}

	public String getPeerProtocol() {
		return this.encapsulatedChannel.getPeerProtocol();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTransport()
	 */
	@Override
	public String getTransport() {
		return encapsulatedChannel.getTransport();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isReliable()
	 */
	@Override
	public boolean isReliable() {
		return encapsulatedChannel.isReliable();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getViaHeader()
	 */
	@Override
	public Via getViaHeader() {
		// Via header of the encapsulated channel
		Via channelViaHeader;

		// Add the branch parameter to the underlying channel's Via header
		channelViaHeader = encapsulatedChannel.getViaHeader();
		try {
			channelViaHeader.setBranch(branch);
		} catch(ParseException ex) {
			logger.log(Level.FINEST, "silently ignoring exception", ex);
		}

		return channelViaHeader;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#sendMessage(gov.nist.javax.sip.message.SIPMessage)
	 */
	@Override
	public void sendMessage(SIPMessage messageToSend) throws IOException {
		// Use the peer address, port and transport that was specified when the transaction was created.

		try {
			RawMessageChannel channel = (RawMessageChannel) encapsulatedChannel;

			for(MessageProcessor messageProcessor : sipStack.getMessageProcessors()) {
				boolean addrmatch = messageProcessor.getIpAddress().getHostAddress().equals(this.getPeerAddress());

				if(addrmatch && messageProcessor.getPort() == this.getPeerPort()
						&& messageProcessor.getTransport().equalsIgnoreCase(this.getPeerProtocol())) {
					if(channel instanceof TCPMessageChannel) {
						try {
							Runnable processMessageTask = () -> {
								try {
									((TCPMessageChannel) channel).processMessage((SIPMessage) messageToSend.clone(),
											getPeerInetAddress());
								} catch(Exception ex) {
									logger.log(Level.SEVERE, "Error self routing TCP message cause by:", ex);
								}
							};

							getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);
						} catch(Exception e) {
							logger.log(Level.SEVERE, "Error passing message in self routing TCP", e);
						}

						logger.log(Level.FINEST, "Self routing message TCP");

						return;
					}

					if(channel instanceof TLSMessageChannel) {
						try {
							Runnable processMessageTask = () -> {
								try {
									((TLSMessageChannel) channel).processMessage((SIPMessage) messageToSend.clone(),
											getPeerInetAddress());
								} catch(Exception ex) {
									logger.log(Level.SEVERE, "Error self routing TLS message cause by", ex);
								}
							};

							getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);
						} catch(Exception e) {
							logger.log(Level.SEVERE, "Error passing message in TLS self routing", e);
						}

						logger.log(Level.FINEST, "Self routing message TLS");

						return;
					}

					if(channel instanceof RawMessageChannel) {
						try {
							Runnable processMessageTask = () -> {
								try {
									channel.processMessage((SIPMessage) messageToSend.clone());
								} catch(Exception ex) {
									logger.log(Level.SEVERE, "Error self routing message cause by", ex);
								}
							};

							getSIPStack().getSelfRoutingThreadpoolExecutor().execute(processMessageTask);
						} catch(Exception e) {
							logger.log(Level.SEVERE, "Error passing message in self routing", e);
						}

						logger.log(Level.FINEST, "Self routing message");

						return;
					}
				}
			}

			encapsulatedChannel.sendMessage(messageToSend, this.getPeerInetAddress(), this.getPeerPort());
		} finally {
			this.startTransactionTimer();
		}
	}

	/**
	 * Parse the byte array as a message, process it through the transaction, and
	 * send it to the SIP peer. This is just a placeholder method -- calling it will
	 * result in an IO exception.
	 *
	 * @param messageBytes    Bytes of the message to send.
	 * @param receiverAddress Address of the target peer.
	 * @param receiverPort    Network port of the target peer.
	 *
	 * @throws IOException If called.
	 */
	public void sendMessage(byte[] messageBytes, InetAddress receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		throw new IOException("Cannot send unparsed message through Transaction Channel!");
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#addEventListener(gov.nist.javax.sip.stack.SIPTransactionEventListener)
	 */
	@Override
	public void addEventListener(SIPTransactionEventListener newListener) {
		eventListeners.add(newListener);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#removeEventListener(gov.nist.javax.sip.stack.SIPTransactionEventListener)
	 */
	@Override
	public void removeEventListener(SIPTransactionEventListener oldListener) {
		eventListeners.remove(oldListener);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#raiseErrorEvent(int)
	 */
	@Override
	public void raiseErrorEvent(int errorEventID) {
		// Error event to send to all listeners
		SIPTransactionErrorEvent newErrorEvent;
		// Iterator through the list of listeners
		Iterator<SIPTransactionEventListener> listenerIterator;
		// Next listener in the list
		SIPTransactionEventListener nextListener;

		// Create the error event
		newErrorEvent = new SIPTransactionErrorEvent(this, errorEventID);

		// Loop through all listeners of this transaction
		synchronized(eventListeners) {
			listenerIterator = eventListeners.iterator();

			while(listenerIterator.hasNext()) {
				// Send the event to the next listener
				nextListener = listenerIterator.next();

				nextListener.transactionErrorEvent(newErrorEvent);
			}
		}

		// Clear the event listeners after propagating the error.
		// Retransmit notifications are just an alert to the
		// application (they are not an error).
		if(errorEventID != SIPTransactionErrorEvent.TIMEOUT_RETRANSMIT) {
			eventListeners.clear();

			// Errors always terminate a transaction
			this.setState(TransactionState.TERMINATED_VALUE);

			if(this instanceof SIPServerTransaction && this.isByeTransaction() && this.getDialog() != null) {
				((SIPDialog) this.getDialog()).setState(SIPDialog.TERMINATED_STATE);
			}
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isServerTransaction()
	 */
	@Override
	public boolean isServerTransaction() {
		return this instanceof SIPServerTransaction;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getRetransmitTimer()
	 */
	@Override
	public int getRetransmitTimer() {
		return SIPTransactionStack.BASE_TIMER_INTERVAL;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getViaHost()
	 */
	@Override
	public String getViaHost() {
		return this.getViaHeader().getHost();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getLastResponse()
	 */
	@Override
	public SIPResponse getLastResponse() {
		return this.lastResponse;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getResponse()
	 */
	@Override
	public Response getResponse() {
		return this.lastResponse;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTransactionId()
	 */
	@Override
	public String getTransactionId() {
		return this.transactionId;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#hashCode()
	 */
	@Override
	public int hashCode() {
		if(this.transactionId == null) {
			return -1;
		}

		return this.transactionId.hashCode();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getViaPort()
	 */
	@Override
	public int getViaPort() {
		return this.getViaHeader().getPort();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#doesCancelMatchTransaction(gov.nist.javax.sip.message.SIPRequest)
	 */
	@Override
	public boolean doesCancelMatchTransaction(SIPRequest requestToTest) {
		// Topmost Via header in the list
		Via topViaHeader;

		// Branch code in the topmost Via header
		String messageBranch;

		// Flags whether the select message is part of this transaction
		boolean transactionMatches;

		transactionMatches = false;

		SIPRequest origRequest = getOriginalRequest();
		if(origRequest == null || this.getMethod().equals(Request.CANCEL)) {
			return false;
		}

		// Get the topmost Via header and its branch parameter
		topViaHeader = requestToTest.getTopmostVia();

		if(topViaHeader != null) {
			messageBranch = topViaHeader.getBranch();

			// If the branch parameter exists but does not start with the magic cookie,
			if(messageBranch != null && messageBranch.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
				// Flags this as old (RFC2543-compatible) client version
				messageBranch = null;
			}

			// If a new branch parameter exists,
			if(messageBranch != null && this.getBranch() != null) {
				// If the branch equals the branch in this message,
				if(getBranch().equalsIgnoreCase(messageBranch)
						&& topViaHeader.getSentBy().equals(origRequest.getTopmostVia().getSentBy())) {
					transactionMatches = true;

					logger.log(Level.FINEST, "returning  true");
				}
			} else {
				/*
				 * If this is an RFC2543-compliant message, If RequestURI, To tag, From tag, CallID, CSeq number,
				 * and top Via headers are the same,
				 */
				logger.log(Level.FINEST, "testing against: {0}", origRequest);

				if (origRequest.getRequestURI().equals(requestToTest.getRequestURI())
						&& origRequest.getTo().equals(requestToTest.getTo())
						&& origRequest.getFrom().equals(requestToTest.getFrom())
						&& origRequest.getCallId().getCallId().equals(requestToTest.getCallId().getCallId())
						&& origRequest.getCSeq().getSeqNumber() == requestToTest.getCSeq().getSeqNumber()
						&& topViaHeader.equals(origRequest.getTopmostVia())) {

					transactionMatches = true;
				}
			}
		}

		return transactionMatches;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setRetransmitTimer(int)
	 */
	@Override
	public void setRetransmitTimer(int retransmitTimer) {
		if(retransmitTimer <= 0) {
			throw new IllegalArgumentException("Retransmit timer must be positive!");
		}

		if(this.transactionTimerStarted.get()) {
			throw new IllegalStateException("Transaction timer is already started");
		}

		baseTimerInterval = retransmitTimer;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#close()
	 */
	@Override
	public void close() {
		this.encapsulatedChannel.close();

		logger.log(Level.FINEST, "Closing: {0}", this.encapsulatedChannel);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isSecure()
	 */
	@Override
	public boolean isSecure() {
		return encapsulatedChannel.isSecure();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getMessageProcessor()
	 */
	@Override
	public MessageProcessor getMessageProcessor() {
		return this.encapsulatedChannel.getMessageProcessor();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setApplicationData(java.lang.Object)
	 */
	@Override
	public void setApplicationData(Object applicationData) {
		this.applicationData = applicationData;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getApplicationData()
	 */
	@Override
	public Object getApplicationData() {
		return this.applicationData;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setEncapsulatedChannel(gov.nist.javax.sip.stack.MessageChannel)
	 */
	@Override
	public void setEncapsulatedChannel(MessageChannel messageChannel) {
		this.encapsulatedChannel = messageChannel;

		if(this instanceof SIPClientTransaction) {
			this.encapsulatedChannel.setEncapsulatedClientTransaction((SIPClientTransaction) this);
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getSipProvider()
	 */
	@Override
	public SipProviderImpl getSipProvider() {
		return this.getMessageProcessor().getListeningPoint().getProvider();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#raiseIOExceptionEvent()
	 */
	@Override
	public void raiseIOExceptionEvent() {
		setState(TransactionState.TERMINATED_VALUE);
	
		String host = getPeerAddress();
		int port = getPeerPort();
		String transport = getTransport();

		IOExceptionEvent exceptionEvent = new IOExceptionEvent(this, host, port, transport);

		getSipProvider().handleEvent(exceptionEvent, this);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#acquireSem()
	 */
	@Override
	public boolean acquireSem() {
		boolean retval = false;

		logger.log(Level.FINEST, "acquireSem [[[[{0}", this);

		if(this.sipStack.maxListenerResponseTime == -1) {
			retval = this.semaphore.acquire();
		} else {
			retval = this.semaphore.tryAcquire();
		}

		logger.log(Level.FINEST, "acquireSem() returning: {0}", retval);

		return retval;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#releaseSem()
	 */
	@Override
	public void releaseSem() {
		try {
			this.toListener = false;
			this.semRelease();
		} catch(Exception ex) {
			logger.log(Level.SEVERE, "Unexpected exception releasing sem", ex);
		}
	}

	@Override
	public void semRelease() {
		logger.log(Level.FINEST, "semRelease ]]]] {0}", this);

		this.semaphore.release();
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#passToListener()
	 */
	@Override
	public boolean passToListener() {
		return toListener;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setPassToListener()
	 */
	@Override
	public void setPassToListener() {
		logger.log(Level.FINEST, "setPassToListener()");

		this.toListener = true;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#testAndSetTransactionTerminatedEvent()
	 */
	@Override
	public synchronized boolean testAndSetTransactionTerminatedEvent() {
		boolean retval = !this.terminatedEventDelivered;

		this.terminatedEventDelivered = true;

		return retval;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getCipherSuite()
	 */
	@Override
	public String getCipherSuite() {
		if(this.getMessageChannel() instanceof TLSMessageChannel) {
			if(((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
					.getHandshakeCompletedEvent() == null) {
				return null;
			}

			return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
						.getHandshakeCompletedEvent().getCipherSuite();
		}

		// Added for https://java.net/jira/browse/JSIP-483
		if(this.getMessageChannel() instanceof NioTlsMessageChannel) {
			if (((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener().getCipherSuite();
		}

		throw new UnsupportedOperationException(NOT_A_TLS_CHANNEL);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getLocalCertificates()
	 */
	@Override
	public Certificate[] getLocalCertificates() {
		if(this.getMessageChannel() instanceof TLSMessageChannel) {
			if(((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			if(((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
					.getHandshakeCompletedEvent() == null) {
				return null;
			}

			return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
					.getHandshakeCompletedEvent().getLocalCertificates();
		}

		// Added for https://java.net/jira/browse/JSIP-483
		if(this.getMessageChannel() instanceof NioTlsMessageChannel) {
			if(((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
						.getLocalCertificates();
		}

		throw new UnsupportedOperationException(NOT_A_TLS_CHANNEL);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getPeerCertificates()
	 */
	@Override
	public java.security.cert.Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
		if(this.getMessageChannel() instanceof TLSMessageChannel) {
			if(((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			if (((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
					.getHandshakeCompletedEvent() == null) {
				return null;
			}

			return ((TLSMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
						.getHandshakeCompletedEvent().getPeerCertificates();
		}

		// Added for https://java.net/jira/browse/JSIP-483
		if (this.getMessageChannel() instanceof NioTlsMessageChannel) {
			if (((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener() == null) {
				return null;
			}

			return ((NioTlsMessageChannel) this.getMessageChannel()).getHandshakeCompletedListener()
					.getPeerCertificates();
		}

		throw new UnsupportedOperationException(NOT_A_TLS_CHANNEL);
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#extractCertIdentities()
	 */
	@Override
	public List<String> extractCertIdentities() throws SSLPeerUnverifiedException {
		if (this.getMessageChannel() instanceof TLSMessageChannel
				|| this.getMessageChannel() instanceof NioTlsMessageChannel) {
			List<String> certIdentities = new ArrayList<>();
			Certificate[] certs = getPeerCertificates();

			if(certs == null) {
				logger.log(Level.FINEST, "No certificates available");

				return certIdentities;
			}

			for(Certificate cert : certs) {
				X509Certificate x509cert = (X509Certificate) cert;
				Collection<List<?>> subjAltNames = null;
				try {
					subjAltNames = x509cert.getSubjectAlternativeNames();
				} catch(CertificateParsingException ex) {
					logger.log(Level.SEVERE, "Error parsing TLS certificate", ex);
				}

				// subjAltName types are defined in rfc2459
				Integer dnsNameType = 2;
				Integer uriNameType = 6;
				if(subjAltNames != null) {
					logger.log(Level.FINEST, "found subjAltNames: {0}", subjAltNames);

					// First look for a URI in the subjectAltName field
					for(List<?> altName : subjAltNames) {
						// 0th position is the alt name type
						// 1st position is the alt name data
						if(altName.get(0).equals(uriNameType)) {
							SipURI altNameUri;
							try {
								altNameUri = new AddressFactoryImpl().createSipURI((String) altName.get(1));
								// only sip URIs are allowed
								if (!"sip".equals(altNameUri.getScheme()))
									continue;
								// user certificates are not allowed
								if (altNameUri.getUser() != null)
									continue;
								String altHostName = altNameUri.getHost();

								logger.log(Level.FINEST, "found uri: {0}, hostName: {1}",
										new Object[] {altName.get(1), altHostName});

								certIdentities.add(altHostName);
							} catch(ParseException e) {
								logger.log(Level.SEVERE, "certificate contains invalid uri: {0}", altName.get(1));
							}
						}
					}

					/*
					 * DNS An implementation MUST accept a domain name system identifier as a SIP domain identity
					 * if and only if no other identity is found that matches the "sip" URI type described above.
					 */
					if(certIdentities.isEmpty()) {
						for(List<?> altName : subjAltNames) {
							if(altName.get(0).equals(dnsNameType)) {
								logger.log(Level.FINEST, "found dns {0}", altName.get(1));

								certIdentities.add(altName.get(1).toString());
							}
						}
					}
				} else {
					/*
					 * If and only if the subjectAltName does not appear in the certificate,
					 * the implementation MAY examine the CN field of the certificate.
					 * If a valid DNS name is found there, the implementation MAY accept this value
					 * as a SIP domain identity.
					 */
					String dname = x509cert.getSubjectDN().getName();
					String cname = "";

					try {
						Matcher matcher = EXTRACT_CN.matcher(dname);

						if(matcher.matches()) {
							cname = matcher.group(1);

							logger.log(Level.FINEST, "found CN: {0} from DN: {1}", new Object[] { cname, dname });

							certIdentities.add(cname);
						}
					} catch(Exception ex) {
						logger.log(Level.SEVERE, "exception while extracting CN", ex);
					}
				}
			}

			return certIdentities;
		}

		throw new UnsupportedOperationException(NOT_A_TLS_CHANNEL);
	}

	/**
	 * @see gov.nist.javax.sip.DialogExt#isReleaseReferences()
	 * @see gov.nist.javax.sip.stack.SIPTransaction#isReleaseReferences()
	 */
	@Override
	public boolean isReleaseReferences() {
		return releaseReferences;
	}

	/**
	 * @see gov.nist.javax.sip.DialogExt#setReleaseReferences(boolean)
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setReleaseReferences(boolean)
	 */
	@Override
	public void setReleaseReferences(boolean releaseReferences) {
		this.releaseReferences = releaseReferences;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTimerD()
	 */
	@Override
	public int getTimerD() {
		return timerD;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTimerT2()
	 */
	@Override
	public int getTimerT2() {
		return t2;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTimerT4()
	 */
	@Override
	public int getTimerT4() {
		return t4;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setTimerD(int)
	 */
	@Override
	public void setTimerD(int interval) {
		if(interval < 32000) {
			throw new IllegalArgumentException("To be RFC 3261 compliant, the value of Timer D should be at least 32s");
		}

		timerD = interval / baseTimerInterval;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setTimerT2(int)
	 */
	@Override
	public void setTimerT2(int interval) {
		t2 = interval / baseTimerInterval;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setTimerT4(int)
	 */
	@Override
	public void setTimerT4(int interval) {
		t4 = interval / baseTimerInterval;

		timerI = t4;
		timerK = t4;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getBaseTimerInterval()
	 */
	@Override
	public int getBaseTimerInterval() {
		return this.baseTimerInterval;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getT4()
	 */
	@Override
	public int getT4() {
		return t4;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getT2()
	 */
	@Override
	public int getT2() {
		return t2;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTimerI()
	 */
	@Override
	public int getTimerI() {
		return this.timerI;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getTimerK()
	 */
	@Override
	public int getTimerK() {
		return this.timerK;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setForkId(java.lang.String)
	 */
	@Override
	public void setForkId(String forkId) {
		this.forkId = forkId;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getForkId()
	 */
	@Override
	public String getForkId() {
		return forkId;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#scheduleMaxTxLifeTimeTimer()
	 */
	@Override
	public void scheduleMaxTxLifeTimeTimer() {
		if(maxTxLifeTimeListener == null && this.getMethod().equalsIgnoreCase(Request.INVITE)
				&& sipStack.getMaxTxLifetimeInvite() > 0) {
			logger.log(Level.FINEST, "Scheduling MaxTxLifeTimeListener for TX: {0}, tx id: {1}, state: {2}",
					new Object[] { this, getTransactionId(), getState() });

			maxTxLifeTimeListener = new MaxTxLifeTimeListener();

			sipStack.getTimer().schedule(maxTxLifeTimeListener, sipStack.getMaxTxLifetimeInvite() * 1000L);
		}

		if(maxTxLifeTimeListener == null && !this.getMethod().equalsIgnoreCase(Request.INVITE)
				&& sipStack.getMaxTxLifetimeNonInvite() > 0) {
			logger.log(Level.FINEST, "Scheduling MaxTxLifeTimeListener for TX: {0}, TX id: {1}, state: {2}",
					new Object[] { getState(), getTransactionId(), getState() });

			maxTxLifeTimeListener = new MaxTxLifeTimeListener();

			sipStack.getTimer().schedule(maxTxLifeTimeListener, sipStack.getMaxTxLifetimeNonInvite() * 1000);
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#cancelMaxTxLifeTimeTimer()
	 */
	@Override
	public void cancelMaxTxLifeTimeTimer() {
		if(maxTxLifeTimeListener != null) {
			logger.log(Level.FINEST, "Cancelling MaxTxLifeTimeListener for TX: {0}, TX ID: {1}, state: {2}",
					new Object[] { this, getTransactionId(), getState() });

			sipStack.getTimer().cancel(maxTxLifeTimeListener);

			maxTxLifeTimeListener = null;
		}
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getMergeId()
	 */
	@Override
	public String getMergeId() {
		if(null == mergeId) {
			return ((SIPRequest) getRequest()).getMergeId();
		}

		return mergeId;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#getAuditTag()
	 */
	@Override
	public long getAuditTag() {
		return auditTag;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setAuditTag(long)
	 */
	@Override
	public void setAuditTag(long auditTag) {
		this.auditTag = auditTag;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPServerTransaction#isTransactionMapped()
	 */
	@Override
	public boolean isTransactionMapped() {
		return this.isMapped;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPServerTransaction#setTransactionMapped(boolean)
	 */
	@Override
	public void setTransactionMapped(boolean transactionMapped) {
		isMapped = transactionMapped;
	}

	/**
	 * @see gov.nist.javax.sip.stack.SIPTransaction#setCollectionTime(int)
	 */
	@Override
	public void setCollectionTime(int collectionTime) {
		this.collectionTime = collectionTime;
	}
}
