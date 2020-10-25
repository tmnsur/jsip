package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.NameValueList;
import gov.nist.javax.sip.DialogExt;
import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.SipListenerExt;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.RAck;
import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.Reason;
import gov.nist.javax.sip.header.RecordRoute;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.Require;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.TimeStamp;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.AddressParser;
import gov.nist.javax.sip.parser.CallIDParser;
import gov.nist.javax.sip.parser.ContactParser;
import gov.nist.javax.sip.parser.RecordRouteParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogDoesNotExistException;
import javax.sip.DialogState;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.OptionTag;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.RAckHeader;
import javax.sip.header.RSeqHeader;
import javax.sip.header.ReasonHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Tracks dialogs. A dialog is a peer to peer association of communicating SIP
 * entities. For INVITE transactions, a Dialog is created when a success message
 * is received (i.e. a response that has a To tag). The SIP Protocol stores
 * enough state in the message structure to extract a dialog identifier that can
 * be used to retrieve this structure from the SipStack.
 */
public class SIPDialog implements javax.sip.Dialog, DialogExt {
	private static final long serialVersionUID = -1429794423085204069L;

	private static final Logger logger = Logger.getLogger(SIPDialog.class.getName());

	private transient boolean dialogTerminatedEventDelivered; // prevent duplicate
	private transient String stackTrace; // for semaphore debugging.
	protected String method;
	// delivery of the event
	protected transient boolean isAssigned;
	protected boolean reInviteFlag;

	// Opaque pointer to application
	private transient Object applicationData;
	// data.

	private transient SIPRequest originalRequest;
	// avoid keeping the original request ref above for too long (mem saving)
	protected transient String originalRequestRecordRouteHeadersString;
	protected transient RecordRouteList originalRequestRecordRouteHeaders;

	/*
	 * Last response (either sent or received). replaced the last response with only
	 * the data from it needed to save on mem
	 */
	protected String lastResponseDialogId;
	private Via lastResponseTopMostVia;
	protected Integer lastResponseStatusCode;
	protected long lastResponseCSeqNumber;
	protected long lastInviteResponseCSeqNumber;
	protected int lastInviteResponseCode;
	protected String lastResponseMethod;
	protected String lastResponseFromTag;
	protected String lastResponseToTag;

	/*
	 * needed for reliable response sending but set to null right after the ACK has been received or sent to let go of
	 * the REF ASAP
	 */
	protected SIPTransaction firstTransaction;

	/*
	 * needed for checking 491 but set to null right after the ACK has been received or sent to let go of the REF ASAP
	 */
	protected SIPTransaction lastTransaction;
	protected String dialogId;
	protected transient String earlyDialogId;
	protected long localSequenceNumber;
	protected long remoteSequenceNumber;
	protected String myTag;
	protected String hisTag;
	protected RouteList routeList;
	private transient SIPTransactionStack sipStack;
	private int dialogState;
	protected transient SIPRequest lastAckSent;

	// jeand : replaced the lastAckReceived message with only the data needed to
	// save on mem
	protected Long lastAckReceivedCSeqNumber;

	// could be set on recovery by examining the method looks like a duplicate
	// of ackSeen
	protected transient boolean ackProcessed;

	protected transient DialogTimerTask timerTask;

	protected transient long nextSeqno;

	private transient int retransmissionTicksLeft;

	private transient int prevRetransmissionTicks;

	protected long originalLocalSequenceNumber;

	// This is for debugging only.
	private transient int ackLine;

	// Audit tag used by the SIP Stack audit
	public transient long auditTag = 0;

	// The following fields are extracted from the request that created the
	// Dialog.

	protected javax.sip.address.Address localParty;
	protected String localPartyStringified;

	protected javax.sip.address.Address remoteParty;
	protected String remotePartyStringified;

	protected CallIdHeader callIdHeader;
	protected String callIdHeaderString;

	public static final int NULL_STATE = -1;
	public static final int EARLY_STATE = DialogState.EARLY_VALUE;
	public static final int CONFIRMED_STATE = DialogState.CONFIRMED_VALUE;
	public static final int TERMINATED_STATE = DialogState.TERMINATED_VALUE;

	// the amount of time to keep this dialog around before the stack GC's it
	private static final int DIALOG_LINGER_TIME = 8;

	protected boolean serverTransactionFlag;
	private transient SipProviderImpl sipProvider;
	protected boolean terminateOnBye;
	protected transient boolean byeSent; // Flag set when BYE is sent, to
	// disallow new

	// requests
	protected Address remoteTarget;
	protected String remoteTargetStringified;
	protected EventHeader eventHeader;
	// for Subscribe notify

	// Stores the last OK for the INVITE
	// Used in createAck.
	protected transient long lastInviteOkReceived;

	private transient Semaphore ackSem = new Semaphore(1);
	protected transient int reInviteWaitTime = 100;
	private transient DialogDeleteTask dialogDeleteTask;
	private transient DialogDeleteIfNoAckSentTask dialogDeleteIfNoAckSentTask;
	protected transient boolean isAcknowledged;
	private transient long highestSequenceNumberAcknowledged = -1;
	protected boolean isBackToBackUserAgent;
	protected boolean sequenceNumberValidation = true;

	// List of event listeners for this dialog
	private transient Set<SIPDialogEventListener> eventListeners;

	// added for Issue 248 : https://jain-sip.dev.java.net/issues/show_bug.cgi?id=248
	private transient Semaphore timerTaskLock = new Semaphore(1);

	/*
	 * We store here the useful data from the first transaction without having to keep the whole transaction object
	 * for the duration of the dialog. It also contains the non-transient information used in the replication of
	 * dialogs.
	 */
	protected boolean firstTransactionSecure;
	protected boolean firstTransactionSeen;
	protected String firstTransactionMethod;
	protected String firstTransactionId;
	protected boolean firstTransactionIsServerTransaction;
	protected String firstTransactionMergeId;
	protected int firstTransactionPort = 5060;
	protected Contact contactHeader;
	protected String contactHeaderStringified;

	private boolean pendingRouteUpdateOn202Response;

	// For subsequent requests.
	protected ProxyAuthorizationHeader proxyAuthorizationHeader;
	// aggressive flag to optimize eagerly
	private boolean releaseReferences;
	private transient EarlyStateTimerTask earlyStateTimerTask;
	private int earlyDialogTimeout = 180;
	private int ackSemTakenFor;
	private Set<String> responsesReceivedInForkingCase = new HashSet<>(0);
	private SIPDialog originalDialog;
	private transient AckSendingStrategy ackSendingStrategy = new AckSendingStrategyImpl();

	public class AckSendingStrategyImpl implements AckSendingStrategy {
		private Hop hop = null;

		@Override
		public void send(SIPRequest ackRequest) throws SipException, IOException {
			hop = sipStack.getNextHop(ackRequest);

			if(hop == null) {
				throw new SipException("No route!");
			}

			logger.log(Level.FINEST, "hop: {0}", hop);

			ListeningPointImpl lp = (ListeningPointImpl) sipProvider.getListeningPoint(hop.getTransport());

			if(null == lp) {
				throw new SipException("No listening point for this provider registered at " + hop);
			}

			InetAddress inetAddress = InetAddress.getByName(hop.getHost());
			MessageChannel messageChannel = lp.getMessageProcessor().createMessageChannel(inetAddress, hop.getPort());

			messageChannel.sendMessage(ackRequest);
		}

		@Override
		public Hop getLastHop() {
			return hop;
		}
	}

	class EarlyStateTimerTask extends SIPStackTimerTask implements Serializable {
		@Override
		public void runTask() {
			try {
				if(SIPDialog.this.getState().equals(DialogState.EARLY)) {

					SIPDialog.this.raiseErrorEvent(SIPDialogErrorEvent.EARLY_STATE_TIMEOUT);
				} else {
					logger.log(Level.FINEST, "EarlyStateTimerTask : Dialog state is {0}", SIPDialog.this.getState());
				}
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Unexpected exception delivering event", ex);
			}
		}

	}

	/**
	 * This task waits till a pending ACK has been recorded and then sends out a
	 * re-INVITE. This is to prevent interleaving INVITEs ( which will result in a
	 * 493 from the UA that receives the out of order INVITE). This is primarily for
	 * B2BUA support. A B2BUA may send a delayed ACK while it does mid call codec
	 * renegotiation. In the meanwhile, it cannot send an intervening re-INVITE
	 * otherwise the othr end will respond with a REQUEST_PENDING. We want to avoid
	 * this condition. Hence we wait till the ACK for the previous re-INVITE has
	 * been sent before sending the next re-INVITE.
	 */
	public class ReInviteSender implements Runnable, Serializable {
		private static final long serialVersionUID = 1019346148741070635L;
		ClientTransaction ctx;

		public void terminate() {
			try {
				logger.log(Level.FINEST, "ReInviteSender::terminate: ctx: {0}", ctx);

				ctx.terminate();

				Thread.currentThread().interrupt();
			} catch(ObjectInUseException e) {
				logger.log(Level.SEVERE, "unexpected error", e);
			}
		}

		public ReInviteSender(ClientTransaction ctx) {
			this.ctx = ctx;
	
			logger.log(Level.FINEST, "ReInviteSender::ReInviteSender: ctx: {0}", ctx);
		}

		@Override
		public void run() {
			try {
				long timeToWait = 0;
				long startTime = System.currentTimeMillis();
				boolean dialogTimedOut = false;

				// If we have an INVITE transaction, make sure that it is TERMINATED
				// before sending a re-INVITE.. Not the cleanest solution but it works.
				logger.log(Level.FINEST, "SIPDialog::reInviteSender: dialog: {0}, lastTransaction: {1},"
						+ " lastTransactionState: {2}", new Object[] { ctx.getDialog(), lastTransaction,
								lastTransaction.getState()});

				if(SIPDialog.this.lastTransaction instanceof SIPServerTransaction
						&& SIPDialog.this.lastTransaction.isInviteTransaction()
						&& SIPDialog.this.lastTransaction.getState() != TransactionState.TERMINATED) {
					((SIPServerTransaction) SIPDialog.this.lastTransaction).waitForTermination();

					Thread.sleep(50);
				}

				if(!SIPDialog.this.takeAckSem()) {
					/*
					 * Could not send re-INVITE fire a timeout on the INVITE.
					 */
					logger.log(Level.SEVERE, "Could not send re-INVITE time out ClientTransaction");

					((SIPClientTransaction) ctx).fireTimeoutTimer();

					/*
					 * Send BYE to the Dialog.
					 */
					if(sipProvider.getSipListener() instanceof SipListenerExt) {
						dialogTimedOut = true;

						raiseErrorEvent(SIPDialogErrorEvent.DIALOG_REINVITE_TIMEOUT, (SIPClientTransaction) ctx);
					} else {
						Request byeRequest = SIPDialog.this.createRequest(Request.BYE);

						if(MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
							byeRequest.addHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
						}

						ReasonHeader reasonHeader = new Reason();

						reasonHeader.setCause(1024);
						reasonHeader.setText("Timed out waiting to re-INVITE");

						byeRequest.addHeader(reasonHeader);

						ClientTransaction byeCtx = SIPDialog.this.getSipProvider().getNewClientTransaction(byeRequest);

						SIPDialog.this.sendRequest(byeCtx);

						return;
					}
				}

				if(getState() != DialogState.TERMINATED) {
					timeToWait = System.currentTimeMillis() - startTime;
				}

				/*
				 * If we had to wait for ACK then wait for the ACK to actually get to the other
				 * side. Wait for any ACK retransmissions to finish. Then send out the request.
				 * This is a hack in support of some UA that want re-INVITEs to be spaced out in
				 * time ( else they return a 400 error code ).
				 */
				try {
					if(timeToWait != 0) {
						Thread.sleep(SIPDialog.this.reInviteWaitTime);
					}
				} catch(InterruptedException ex) {
					logger.log(Level.FINEST, "Interrupted sleep");

					return;
				}

				if(SIPDialog.this.getState() != DialogState.TERMINATED && !dialogTimedOut
						&& ctx.getState() != TransactionState.TERMINATED) {
					SIPDialog.this.sendRequest(ctx, true);

					logger.log(Level.FINEST, "re-INVITE successfully sent");
				}
			} catch(Exception ex) {
				logger.log(Level.SEVERE, "Error sending re-INVITE", ex);
			} finally {
				this.ctx = null;
			}
		}
	}

	class LingerTimer extends SIPStackTimerTask implements Serializable {
		public void runTask() {
			SIPDialog dialog = SIPDialog.this;
			sipStack.removeDialog(dialog);
			/*
			 * Issue 279 : https://jain-sip.dev.java.net/issues/show_bug.cgi?id=279 if non reentrant listener is used
			 * the event delivery of DialogTerminated can happen after the clean
			 */
			if(((SipStackImpl) getStack()).isReEntrantListener()) {
				cleanUp();
			}
		}
	}

	class DialogTimerTask extends SIPStackTimerTask implements Serializable {
		int nRetransmissions;

		SIPServerTransaction transaction;

		// long cseqNumber;
		public DialogTimerTask(SIPServerTransaction transaction) {
			this.transaction = transaction;
			this.nRetransmissions = 0;
		}

		public void runTask() {
			// If I ACK has not been seen on Dialog,
			// resend last response.
			SIPDialog dialog = SIPDialog.this;

			logger.log(Level.FINEST, "Running dialog timer");

			nRetransmissions++;

			SIPServerTransaction transaction = this.transaction;

			/*
			 * Issue 106. Section 13.3.1.4 RFC 3261 The 2xx response is passed to the
			 * transport with an interval that starts at T1 seconds and doubles for each
			 * retransmission until it reaches T2 seconds If the server retransmits the 2xx
			 * response for 64T1 seconds without receiving an ACK, the dialog is confirmed,
			 * but the session SHOULD be terminated.
			 */

			if(nRetransmissions > sipStack.getAckTimeoutFactor() * SIPTransaction.T1) {
				if(SIPDialog.this.getSipProvider().getSipListener() != null
						&& SIPDialog.this.getSipProvider().getSipListener() instanceof SipListenerExt) {
					raiseErrorEvent(SIPDialogErrorEvent.DIALOG_ACK_NOT_RECEIVED_TIMEOUT);
				} else {
					SIPDialog.this.delete();
				}

				if(transaction != null && transaction.getState() != javax.sip.TransactionState.TERMINATED) {
					transaction.raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);
				}
			} else if((transaction != null) && (!dialog.isAckSeen())) {
				// Retransmit to 2xx until ack receive dialog.
				if(lastResponseStatusCode.intValue() / 100 == 2) {
					try {
						// resend the last response.
						if(dialog.toRetransmitFinalResponse(transaction.getTimerT2())) {
							transaction.resendLastResponseAsBytes();
						}
					} catch(IOException ex) {
						raiseIOException(transaction.getPeerAddress(), transaction.getPeerPort(),
								transaction.getPeerProtocol());
					} finally {
						/*
						 * Need to fire the timer so transaction will eventually time out whether or not the
						 * IOException occurs Note that this firing also drives Listener timeout.
						 */
						SIPTransactionStack stack = dialog.sipStack;

						logger.log(Level.FINEST, "resend 200 response from: {0}", dialog);

						transaction.fireTimer();
					}
				}
			}

			// Stop running this timer if the dialog is in the confirmed state or ACK seen if retransmit filter on.
			if(dialog.isAckSeen() || dialog.dialogState == TERMINATED_STATE) {
				this.transaction = null;

				getStack().getTimer().cancel(this);
			}
		}

		@Override
		public void cleanUpBeforeCancel() {
			transaction = null;
			cleanUpOnAck();

			super.cleanUpBeforeCancel();
		}
	}

	/**
	 * This timer task is used to garbage collect the dialog after some time.
	 * 
	 */
	class DialogDeleteTask extends SIPStackTimerTask implements Serializable {
		public void runTask() {
			delete();
		}
	}

	/**
	 * This timer task is used to garbage collect the dialog after some time.
	 * 
	 */
	class DialogDeleteIfNoAckSentTask extends SIPStackTimerTask implements Serializable {
		private long seqno;

		public DialogDeleteIfNoAckSentTask(long seqno) {
			this.seqno = seqno;
		}

		public void runTask() {
			if(SIPDialog.this.highestSequenceNumberAcknowledged < seqno) {
				/*
				 * Did not send ACK so we need to delete the dialog. B2BUA NOTE: we may want to
				 * send BYE to the Dialog at this point. Do we want to make this behavior
				 * tailorable?
				 */
				dialogDeleteIfNoAckSentTask = null;

				if(!SIPDialog.this.isBackToBackUserAgent) {
					logger.log(Level.SEVERE, "ACK Was not sent. killing dialog: {0}", dialogId);

					if(sipProvider.getSipListener() instanceof SipListenerExt) {
						raiseErrorEvent(SIPDialogErrorEvent.DIALOG_ACK_NOT_SENT_TIMEOUT);
					} else {
						delete();
					}
				} else {
					logger.log(Level.SEVERE, "ACK Was not sent. Sending BYE: {0}", dialogId);

					if(sipProvider.getSipListener() instanceof SipListenerExt) {
						raiseErrorEvent(SIPDialogErrorEvent.DIALOG_ACK_NOT_SENT_TIMEOUT);
					} else {
						/*
						 * Send BYE to the Dialog. This will be removed for the next spec revision.
						 */
						try {
							Request byeRequest = SIPDialog.this.createRequest(Request.BYE);
							if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
								byeRequest.addHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
							}
							ReasonHeader reasonHeader = new Reason();
							reasonHeader.setProtocol("SIP");
							reasonHeader.setCause(1025);
							reasonHeader.setText("Timed out waiting to send ACK " + dialogId);
							byeRequest.addHeader(reasonHeader);
							ClientTransaction byeCtx = SIPDialog.this.getSipProvider()
									.getNewClientTransaction(byeRequest);
							SIPDialog.this.sendRequest(byeCtx);
						} catch (Exception ex) {
							SIPDialog.this.delete();
						}
					}
				}
			}
		}
	}

	/**
	 * Protected Dialog constructor.
	 */
	private SIPDialog(SipProviderImpl provider) {
		this.terminateOnBye = true;
		this.routeList = new RouteList();

		// not yet initialized.
		this.dialogState = NULL_STATE;

		localSequenceNumber = 0;
		remoteSequenceNumber = -1;

		this.sipProvider = provider;

		eventListeners = new CopyOnWriteArraySet<>();

		this.earlyDialogTimeout = ((SIPTransactionStack) provider.getSipStack()).getEarlyDialogTimeout();
	}

	private void recordStackTrace() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		new Exception().printStackTrace(writer);
		String stackTraceSignature = Integer.toString(Math.abs(new Random().nextInt()));

		logger.log(Level.FINEST, "TraceRecord: {0}", stackTraceSignature);

		this.stackTrace = "TraceRecord = " + stackTraceSignature + ":" + stringWriter.getBuffer().toString();
	}

	/**
	 * Constructor given the first transaction.
	 * 
	 * @param transaction is the first transaction.
	 */
	public SIPDialog(SIPTransaction transaction) {
		this(transaction.getSipProvider());

		SIPRequest sipRequest = (SIPRequest) transaction.getRequest();

		this.callIdHeader = sipRequest.getCallId();
		this.earlyDialogId = sipRequest.getDialogId(false);
		this.sipStack = transaction.getSIPStack();
		this.sipProvider = transaction.getSipProvider();

		if(sipProvider == null) {
			throw new NullPointerException("Null Provider!");
		}

		this.isBackToBackUserAgent = sipStack.isBackToBackUserAgent;

		this.addTransaction(transaction);

		logger.log(Level.FINEST, "Creating a dialog: {0}", this);
		logger.log(Level.FINEST, "provider port: {0}", this.sipProvider.getListeningPoint().getPort());

		addEventListener(sipStack);

		releaseReferences = sipStack.isAggressiveCleanup();
	}

	/**
	 * Constructor given a transaction and a response.
	 * 
	 * @param transaction -- the transaction ( client/server)
	 * @param sipResponse -- response with the appropriate tags.
	 */
	public SIPDialog(SIPClientTransaction transaction, SIPResponse sipResponse) {
		this(transaction);

		if(sipResponse == null) {
			throw new NullPointerException("Null SipResponse");
		}

		this.setLastResponse(transaction, sipResponse);
		this.isBackToBackUserAgent = sipStack.isBackToBackUserAgent;
	}

	/**
	 * create a sip dialog with a response ( no tx)
	 */
	public SIPDialog(SipProviderImpl sipProvider, SIPResponse sipResponse) {
		this(sipProvider);
		this.sipStack = (SIPTransactionStack) sipProvider.getSipStack();
		this.setLastResponse(null, sipResponse);
		this.localSequenceNumber = sipResponse.getCSeq().getSeqNumber();
		this.originalLocalSequenceNumber = localSequenceNumber;
		this.localParty = sipResponse.getFrom().getAddress();
		this.remoteParty = sipResponse.getTo().getAddress();
		this.method = sipResponse.getCSeq().getMethod();
		this.callIdHeader = sipResponse.getCallId();
		this.serverTransactionFlag = false;
		this.setLocalTag(sipResponse.getFrom().getTag());
		this.setRemoteTag(sipResponse.getTo().getTag());

		logger.log(Level.FINEST, "Creating a dialog: {0}", this);

		this.isBackToBackUserAgent = sipStack.isBackToBackUserAgent;

		addEventListener(sipStack);

		releaseReferences = sipStack.isAggressiveCleanup();
	}

	/**
	 * Creates a new dialog based on a received NOTIFY. The dialog state is
	 * initialized appropriately. The NOTIFY differs in the From tag
	 * 
	 * Made this a separate method to clearly distinguish what's happening here -
	 * this is a non-trivial case
	 * 
	 * @param subscribeTx - the transaction started with the SUBSCRIBE that we sent
	 * @param notifyST    - the ServerTransaction created for an incoming NOTIFY
	 * @return -- a new dialog created from the subscribe original SUBSCRIBE
	 *         transaction.
	 */
	public SIPDialog(SIPClientTransaction subscribeTx, SIPTransaction notifyST) {
		this(notifyST);
		//
		// The above sets firstTransaction to NOTIFY (ST), correct that
		//
		serverTransactionFlag = false;
		// they share this one
		lastTransaction = subscribeTx;
		storeFirstTransactionInfo(this, subscribeTx);
		terminateOnBye = false;
		localSequenceNumber = subscribeTx.getCSeq();
		SIPRequest not = (SIPRequest) notifyST.getRequest();
		remoteSequenceNumber = not.getCSeq().getSeqNumber();
		setDialogId(not.getDialogId(true));
		setLocalTag(not.getToTag());
		setRemoteTag(not.getFromTag());
		// to properly create the Dialog object.
		// If not the stack will throw an exception when creating the response.
		setLastResponse(subscribeTx, subscribeTx.getLastResponse());

		// Don't use setLocal / setRemote here, they make other assumptions
		localParty = not.getTo().getAddress();
		remoteParty = not.getFrom().getAddress();

		// initialize d's route set based on the NOTIFY. Any proxies must have Record-Routed
		addRoute(not);

		// set state, *after* setting route set!
		setState(CONFIRMED_STATE);
	}

	/**
	 * A debugging print routine.
	 */
	private void printRouteList() {
		logger.log(Level.FINEST, "this: {0}, printRouteList: {1}", new Object[] {this, this.routeList.encode()});
	}

	/**
	 * Raise an IO exception for asynchronous retransmission of responses
	 * 
	 * @param host     -- host to where the IO was headed
	 * @param port     -- remote port
	 * @param protocol -- protocol (UDP/TCP/TLS)
	 */
	private void raiseIOException(String host, int port, String protocol) {
		// Error occurred in retransmitting response. Deliver the error event to the listener Kill the dialog.

		IOExceptionEvent ioError = new IOExceptionEvent(this, host, port, protocol);

		sipProvider.handleEvent(ioError, null);

		setState(SIPDialog.TERMINATED_STATE);
	}

	/**
	 * Raise a dialog timeout if an ACK has not been sent or received
	 * 
	 * @param dialogTimeoutError
	 */
	private void raiseErrorEvent(int dialogTimeoutError, SIPClientTransaction clientTransaction) {
		// Error event to send to all listeners
		SIPDialogErrorEvent newErrorEvent;

		// Iterator through the list of listeners
		Iterator<SIPDialogEventListener> listenerIterator;
		// Next listener in the list
		SIPDialogEventListener nextListener;

		// Create the error event
		newErrorEvent = new SIPDialogErrorEvent(this, dialogTimeoutError);
		// The client transaction can be retrieved by the application timeout handler when a re-INVITE is being sent.
		newErrorEvent.setClientTransaction(clientTransaction);

		// Loop through all listeners of this transaction
		synchronized(eventListeners) {
			listenerIterator = eventListeners.iterator();

			while(listenerIterator.hasNext()) {
				// Send the event to the next listener
				nextListener = listenerIterator.next();
				nextListener.dialogErrorEvent(newErrorEvent);
			}
		}

		// Clear the event listeners after propagating the error.
		eventListeners.clear();

		/*
		 * Errors always terminate a dialog except if a timeout has occurred because an ACK was not sent or received,
		 * then it is the responsibility of the application to terminate the dialog, either by sending a BYE or by
		 * calling delete() on the dialog
		 */
		if (dialogTimeoutError != SIPDialogErrorEvent.DIALOG_ACK_NOT_SENT_TIMEOUT
				&& dialogTimeoutError != SIPDialogErrorEvent.DIALOG_ACK_NOT_RECEIVED_TIMEOUT
				&& dialogTimeoutError != SIPDialogErrorEvent.EARLY_STATE_TIMEOUT
				&& dialogTimeoutError != SIPDialogErrorEvent.DIALOG_REINVITE_TIMEOUT) {
			delete();
		}

		// we stop the timer in any case
		stopTimer();
	}

	/**
	 * Raise a dialog timeout if an ACK has not been sent or received
	 * 
	 * @param dialogTimeoutError
	 */
	private void raiseErrorEvent(int dialogTimeoutError) {
		raiseErrorEvent(dialogTimeoutError, null);
	}

	/**
	 * Set the remote party for this Dialog.
	 * 
	 * @param sipMessage -- SIP Message to extract the relevant information from.
	 */
	protected void setRemoteParty(SIPMessage sipMessage) {
		if(!isServer()) {
			this.remoteParty = sipMessage.getTo().getAddress();
		} else {
			this.remoteParty = sipMessage.getFrom().getAddress();
		}

		logger.log(Level.FINEST, "settingRemoteParty: {0}", remoteParty);
	}

	/**
	 * Add a route list extracted from a record route list. If this is a server
	 * dialog then we assume that the record are added to the route list IN order.
	 * If this is a client dialog then we assume that the record route headers give
	 * us the route list to add in reverse order.
	 * 
	 * @param recordRouteList -- the record route list from the incoming message.
	 */
	private void addRoute(RecordRouteList recordRouteList) {
		try {
			if(!this.isServer()) {
				/*
				 * This is a client dialog so we extract the record route from the response and reverse its order to
				 * create a route list.
				 */
				this.routeList = new RouteList();

				// start at the end of the list and walk backwards
				ListIterator li = recordRouteList.listIterator(recordRouteList.size());
				while (li.hasPrevious()) {
					RecordRoute rr = (RecordRoute) li.previous();

					Route route = new Route();
					AddressImpl address = ((AddressImpl) ((AddressImpl) rr.getAddress()).clone());

					route.setAddress(address);
					route.setParameters((NameValueList) rr.getParameters().clone());

					this.routeList.add(route);
				}
			} else {
				/*
				 * This is a server dialog. The top most record route header is the one that is closest to us.
				 * We extract the route list in the same order as the addresses in the incoming request.
				 */
				this.routeList = new RouteList();

				ListIterator li = recordRouteList.listIterator();

				while(li.hasNext()) {
					RecordRoute rr = (RecordRoute) li.next();
					Route route = new Route();
					AddressImpl address = ((AddressImpl) ((AddressImpl) rr.getAddress()).clone());

					route.setAddress(address);
					route.setParameters((NameValueList) rr.getParameters().clone());

					routeList.add(route);
				}
			}
		} finally {
			if(logger.isLoggable(Level.WARNING) || logger.isLoggable(Level.FINEST)) {
				Iterator<Route> it = routeList.iterator();

				while(it.hasNext()) {
					SipURI sipUri = (SipURI) (it.next().getAddress().getURI());

					if(!sipUri.hasLrParam()) {
						logger.log(Level.WARNING, "NON LR route in Route set detected for dialog: {0}", this);
					} else {
						logger.log(Level.FINEST, "route: {0}", sipUri);
					}
				}
			}
		}
	}

	/**
	 * Add a route list extacted from the contact list of the incoming message.
	 * 
	 * @param contactList -- contact list extracted from the incoming message.
	 */
	protected void setRemoteTarget(ContactHeader contact) {
		logger.log(Level.FINEST, "Dialog.setRemoteTarget: {0}", contact.getAddress());

		this.remoteTarget = contact.getAddress();
	}

	/**
	 * Extract the route information from this SIP Message and add the relevant
	 * information to the route set.
	 * 
	 * @param sipMessage is the SIP message for which we want to add the route.
	 */
	private synchronized void addRoute(SIPResponse sipResponse) {
		logger.log(Level.FINEST, "setContact, dialogState: {0}, state: {1}", new Object[] {this, this.getState()});

		if(100 == sipResponse.getStatusCode()) {
			// Do nothing for trying messages.
			return;
		} else if(this.dialogState == TERMINATED_STATE) {
			// Do nothing if the dialog state is terminated.
			return;
		} else if(this.dialogState == CONFIRMED_STATE) {
			/*
			 * cannot add route list after the dialog is initialized. Remote target is updated on RE-INVITE
			 * but not the route list.
			 */
			if(sipResponse.getStatusCode() / 100 == 2 && !this.isServer()) {
				ContactList contactList = sipResponse.getContactHeaders();

				if(contactList != null && SIPRequest.isTargetRefresh(sipResponse.getCSeq().getMethod())) {
					this.setRemoteTarget((ContactHeader) contactList.getFirst());
				}
			}

			if(!this.pendingRouteUpdateOn202Response) {
				return;
			}
		}

		// Update route list on response if I am a client dialog.
		if(!isServer() || this.pendingRouteUpdateOn202Response) {
			// only update the route set if the dialog is not in the confirmed state.
			if ((this.getState() != DialogState.CONFIRMED && this.getState() != DialogState.TERMINATED)
					|| this.pendingRouteUpdateOn202Response) {
				RecordRouteList rrlist = sipResponse.getRecordRouteHeaders();

				// Add the route set from the incoming response in reverse order for record route headers.
				if(rrlist != null) {
					this.addRoute(rrlist);
				} else {
					// Set the route list to the last seen route list.
					this.routeList = new RouteList();
				}
			}

			ContactList contactList = sipResponse.getContactHeaders();
			if(contactList != null) {
				this.setRemoteTarget((ContactHeader) contactList.getFirst());
			}
		}
	}

	/**
	 * Get a cloned copy of route list for the Dialog.
	 * 
	 * @return -- a cloned copy of the dialog route list.
	 */
	private synchronized RouteList getRouteList() {
		logger.log(Level.FINEST, "getRouteList: {0}", this);

		// Find the top via in the route list.
		ListIterator li;
		RouteList retval = new RouteList();

		retval = new RouteList();
		if (this.routeList != null) {
			li = routeList.listIterator();
			while (li.hasNext()) {
				Route route = (Route) li.next();
				retval.add((Route) route.clone());
			}
		}

		logger.log(Level.FINEST, "-----\ngetRouteList for {0}", this);
		logger.log(Level.FINEST, "RouteList: {0}", retval.encode());

		if(routeList != null) {
			logger.log(Level.FINEST, "myRouteList: {0}", routeList.encode());
		}

		logger.log(Level.FINEST, "----- ");

		return retval;
	}

	void setRouteList(RouteList routeList) {
		this.routeList = routeList;
	}

	/**
	 * Sends ACK Request to the remote party of this Dialogue.
	 * 
	 * 
	 * @param request                        the new ACK Request message to send.
	 * @param throwIOExceptionAsSipException - throws SipException if IOEx
	 *                                       encountered. Otherwise, no exception is
	 *                                       propagated.
	 * @param releaseAckSem                  - release ACK semaphore.
	 * @throws SipException if implementation cannot send the ACK Request for any other reason
	 */
	private void sendAck(Request request, boolean throwIOExceptionAsSipException) throws SipException {
		SIPRequest ackRequest = (SIPRequest) request;

		logger.log(Level.FINEST, "sendAck: {0}", this);

		if(!ackRequest.getMethod().equals(Request.ACK)) {
			throw new SipException("Bad request method -- should be ACK");
		}

		if(this.getState() == null || this.getState().getValue() == EARLY_STATE) {
			logger.log(Level.SEVERE, "Bad Dialog State for {0} dialogID: {1}", new Object[] {this, this.getDialogId()});

			throw new SipException("Bad dialog state " + this.getState());
		}

		if(!this.getCallId().getCallId().equals(((SIPRequest) request).getCallId().getCallId())) {
			logger.log(Level.SEVERE, "CallID: {0}", this.getCallId());
			logger.log(Level.SEVERE, "RequestCallID: {0}", ackRequest.getCallId().getCallId());
			logger.log(Level.SEVERE, "dialog: {0}", this);

			throw new SipException("Bad call ID in request");
		}

		try {
			logger.log(Level.FINEST, "setting FROM tag For outgoing ACK: {0}", getLocalTag());
			logger.log(Level.FINEST, "setting TO tag for outgoing ACK: {0}", getRemoteTag());
			logger.log(Level.FINEST, "ack: {0}", ackRequest);

			if(this.getLocalTag() != null) {
				ackRequest.getFrom().setTag(this.getLocalTag());
			}

			if(this.getRemoteTag() != null) {
				ackRequest.getTo().setTag(this.getRemoteTag());
			}
		} catch(ParseException ex) {
			throw new SipException(ex.getMessage());
		}

		boolean releaseAckSem = false;
		long cseqNo = ((SIPRequest) request).getCSeq().getSeqNumber();

		if (!this.isAckSent(cseqNo)) {
			releaseAckSem = true;
		}

		/*
		 * we clone the request that we store to make sure that if getNextHop modifies the request we store it as
		 * it was passed to the method originally
		 */
		this.setLastAckSent((SIPRequest) ackRequest.clone());

		try {
			ackSendingStrategy.send(ackRequest);

			// Sent at least one ACK.
			this.isAcknowledged = true;
			this.highestSequenceNumberAcknowledged = Math.max(this.highestSequenceNumberAcknowledged,
					ackRequest.getCSeq().getSeqNumber());

			if(releaseAckSem && this.isBackToBackUserAgent) {
				this.releaseAckSem();
			} else {
				logger.log(Level.FINEST, "Not releasing ack sem for: {0}, isAckSent: {1}",
						new Object[] {this, releaseAckSem});
			}
		} catch (IOException ex) {
			if (throwIOExceptionAsSipException)
				throw new SipException("Could not send ack", ex);
			Hop hop = ackSendingStrategy.getLastHop();
			if (hop == null) {
				hop = sipStack.getNextHop(ackRequest);
			}
			this.raiseIOException(hop.getHost(), hop.getPort(), hop.getTransport());
		} catch(SipException ex) {
			throw ex;
		} catch(Exception ex) {
			throw new SipException("Could not create message channel", ex);
		}

		if(this.dialogDeleteTask != null) {
			this.getStack().getTimer().cancel(dialogDeleteTask);
			this.dialogDeleteTask = null;
		}
	}

	/**
	 * Set the stack address. Prevent us from routing messages to ourselves.
	 * 
	 * @param sipStack the address of the SIP stack.
	 * 
	 */
	void setStack(SIPTransactionStack sipStack) {
		this.sipStack = sipStack;
	}

	/**
	 * Get the stack .
	 * 
	 * @return sipStack the SIP stack of the dialog.
	 * 
	 */
	SIPTransactionStack getStack() {
		return sipStack;
	}

	/**
	 * Return True if this dialog is terminated on BYE.
	 * 
	 */
	boolean isTerminatedOnBye() {
		return this.terminateOnBye;
	}

	/**
	 * Mark that the dialog has seen an ACK.
	 */
	void ackReceived(long cseqNumber) {
		// Suppress retransmission of the final response
		if(this.isAckSeen()) {
			logger.log(Level.FINEST, "Ack already seen for response -- dropping");

			return;
		}

		SIPServerTransaction tr = this.getInviteTransaction();
		if(tr != null) {
			if(tr.getCSeq() == cseqNumber) {
				acquireTimerTaskSem();

				try {
					if(this.timerTask != null) {
						this.getStack().getTimer().cancel(timerTask);

						this.timerTask = null;
					}
				} finally {
					releaseTimerTaskSem();
				}

				if(this.dialogDeleteTask != null) {
					this.getStack().getTimer().cancel(dialogDeleteTask);

					this.dialogDeleteTask = null;
				}

				lastAckReceivedCSeqNumber = Long.valueOf(cseqNumber);

				if(logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "ackReceived for {0}", tr.getMethod());
				}

				if(this.isBackToBackUserAgent) {
					this.releaseAckSem();
				}

				this.setState(CONFIRMED_STATE);
			}
		} else {
			logger.log(Level.FINEST, "tr is null -- not updating the ack state");
		}
	}

	/**
	 * Return true if a terminated event was delivered to the application as a
	 * result of the dialog termination.
	 * 
	 */
	synchronized boolean testAndSetIsDialogTerminatedEventDelivered() {
		boolean retval = this.dialogTerminatedEventDelivered;

		this.dialogTerminatedEventDelivered = true;

		return retval;
	}

	/**
	 * Adds a new event listener to this dialog.
	 * 
	 * @param newListener Listener to add.
	 */
	public void addEventListener(SIPDialogEventListener newListener) {
		eventListeners.add(newListener);
	}

	/**
	 * Removed an event listener from this dialog.
	 * 
	 * @param oldListener Listener to remove.
	 */
	public void removeEventListener(SIPDialogEventListener oldListener) {
		eventListeners.remove(oldListener);
	}

	/*
	 * @see javax.sip.Dialog#setApplicationData()
	 */
	public void setApplicationData(Object applicationData) {
		this.applicationData = applicationData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getApplicationData()
	 */
	public Object getApplicationData() {
		return this.applicationData;
	}

	/**
	 * Updates the next consumable seqno.
	 * 
	 */
	public synchronized void requestConsumed() {
		this.nextSeqno = this.getRemoteSeqNumber() + 1;

		logger.log(Level.FINEST, "Request Consumed -- next consumable Request Seqno: {0}", this.nextSeqno);
	}

	/**
	 * Return true if this request can be consumed by the dialog.
	 * 
	 * @param dialogRequest is the request to check with the dialog.
	 * @return true if the dialogRequest sequence number matches the next consumable sequence no.
	 */
	public synchronized boolean isRequestConsumable(SIPRequest dialogRequest) {
		// have not yet set remote sequence no - this is a fresh
		if(dialogRequest.getMethod().equals(Request.ACK)) {
			throw new RuntimeException("Illegal method");
		}

		// For loose validation this function is delegated to the application
		if(!this.isSequenceNumberValidation()) {
			return true;
		}

		// Acceptable iff remoteCSeq < cseq. remoteCSeq==-1 when not defined yet, so that works too
		return remoteSequenceNumber < dialogRequest.getCSeq().getSeqNumber();
	}

	/**
	 * This method is called when a forked dialog is created from the client side.
	 * It starts a timer task. If the timer task expires before an ACK is sent then
	 * the dialog is cancelled (i.e. garbage collected ).
	 */
	public void doDeferredDelete() {
		if(sipStack.getTimer() == null) {
			this.setState(TERMINATED_STATE);
		} else {
			this.dialogDeleteTask = new DialogDeleteTask();

			// Delete the transaction after the max ACK timeout.
			if(sipStack.getTimer() != null && sipStack.getTimer().isStarted()) {
				int delay = SIPTransactionStack.BASE_TIMER_INTERVAL;

				if(lastTransaction != null) {
					delay = lastTransaction.getBaseTimerInterval();
				}

				sipStack.getTimer().schedule(this.dialogDeleteTask, SIPTransaction.TIMER_H * delay);
			} else {
				this.delete();
			}
		}
	}

	/**
	 * Set the state for this dialog.
	 * 
	 * @param state is the state to set for the dialog.
	 */
	public void setState(int state) {
		logger.log(Level.FINEST, "SIPDialog::setState:Setting dialog state for {0}, newState: {1}",
				new Object[] {this, state});

		if(state != NULL_STATE && state != this.dialogState) {
			logger.log(Level.FINEST, "SIPDialog::setState: {0}, old dialog state is: {1}", new Object[] {
					this, this.getState()});
	
			logger.log(Level.FINEST, "SIPDialog::setState: {0}, New dialog state is: {1}", new Object[] {
					this, DialogState.getObject(state)});
		}

		if(state == EARLY_STATE) {
			this.addEventListener(this.getSipProvider());
		}

		this.dialogState = state;

		// Dialog is in terminated state set it up for GC.
		if(state == TERMINATED_STATE) {
			this.removeEventListener(this.getSipProvider());

			if(sipStack.getTimer() != null && sipStack.getTimer().isStarted()) { // may be null after shutdown
				sipStack.getTimer().schedule(new LingerTimer(), DIALOG_LINGER_TIME * 1000);
			}

			this.stopTimer();
		}
	}

	/**
	 * Debugging print for the dialog.
	 */
	public void printDebugInfo() {
		logger.log(Level.FINEST, "isServer: {0}", isServer());
		logger.log(Level.FINEST, "localTag: {0}", getLocalTag());
		logger.log(Level.FINEST, "remoteTag: {0}", getRemoteTag());
		logger.log(Level.FINEST, "localSequenceNumber: {0}", getLocalSeqNumber());
		logger.log(Level.FINEST, "remoteSequenceNumber: {0}", getRemoteSeqNumber());
		logger.log(Level.FINEST, "ackLine: {0} {1}", new Object[] {this.getRemoteTag(), ackLine});
	}

	/**
	 * Return true if the dialog has already seen the ack.
	 * 
	 * @return flag that records if the ack has been seen.
	 */
	public boolean isAckSeen() {
		if(lastAckReceivedCSeqNumber == null && lastResponseStatusCode == Response.OK) {
			logger.log(Level.FINEST, "SIPDialog::isAckSeen: {0} lastAckReceived is null -- returning false", this);

			return false;
		}

		if(lastResponseMethod == null) {
			logger.log(Level.FINEST, "SIPDialog::isAckSeen: {0} lastResponse is null -- returning false", this);

			return false;
		}

		if(lastAckReceivedCSeqNumber == null && lastResponseStatusCode / 100 > 2) {
			logger.log(Level.FINEST, "SIPDialog::isAckSeen: {0} lastResponse statusCode {1}", new Object[] {
					this, lastResponseStatusCode});

			return true;
		}

		logger.log(Level.FINEST, "SIPDialog::isAckSeen:lastAckReceivedCSeqNumber: {0}, remoteCSeqNumber: {1}",
				new Object[] { lastAckReceivedCSeqNumber, this.getRemoteSeqNumber() });

		return this.lastAckReceivedCSeqNumber != null && this.lastAckReceivedCSeqNumber >= this.getRemoteSeqNumber();
	}

	/**
	 * Get the last ACK for this transaction.
	 */
	public SIPRequest getLastAckSent() {
		return this.lastAckSent;
	}

	/**
	 * Return true if ACK was sent ( for client TX ). For server TX, this is a NO-OP
	 * ( we don't send ACK ).
	 */
	public boolean isAckSent(long cseqNo) {
		if(this.getLastTransaction() == null) {
			return true;
		}

		if(this.getLastTransaction() instanceof ClientTransaction) {
			if(this.getLastAckSent() == null) {
				return false;
			}

			return cseqNo <= ((SIPRequest) this.getLastAckSent()).getCSeq().getSeqNumber();
		}

		return true;
	}

	@Deprecated
	public Transaction getFirstTransaction() {
		throw new UnsupportedOperationException("This method has been deprecated and is no longer supported");
	}

	/**
	 * This is for internal use only.
	 * 
	 */
	public Transaction getFirstTransactionInt() {
		// we try to avoid keeping the ref around for too long to help the GC
		if(firstTransaction != null) {
			return firstTransaction;
		}

		return this.sipStack.findTransaction(firstTransactionId, firstTransactionIsServerTransaction);
	}

	/**
	 * Gets the route set for the dialog. When acting as an User Agent Server the
	 * route set MUST be set to the list of URIs in the Record-Route header field
	 * from the request, taken in order and preserving all URI parameters. When
	 * acting as an User Agent Client the route set MUST be set to the list of URIs
	 * in the Record-Route header field from the response, taken in reverse order
	 * and preserving all URI parameters. If no Record-Route header field is present
	 * in the request or response, the route set MUST be set to the empty set. This
	 * route set, even if empty, overrides any pre-existing route set for future
	 * requests in this dialog.
	 * <p>
	 * Requests within a dialog MAY contain Record-Route and Contact header fields.
	 * However, these requests do not cause the dialog's route set to be modified.
	 * <p>
	 * The User Agent Client uses the remote target and route set to build the
	 * Request-URI and Route header field of the request.
	 * 
	 * @return an Iterator containing a list of route headers to be used for
	 *         forwarding. Empty iterator is returned if route has not been
	 *         established.
	 */
	public Iterator getRouteSet() {
		if(this.routeList == null) {
			return new LinkedList().listIterator();
		}

		return this.getRouteList().listIterator();
	}

	/**
	 * Add a Route list extracted from a SIPRequest to this Dialog.
	 * 
	 * @param sipRequest
	 */
	public synchronized void addRoute(SIPRequest sipRequest) {
		logger.log(Level.FINEST, "setContact: dialogState: {0}, state: {1}", new Object[] {this, this.getState()});

		if(this.dialogState == CONFIRMED_STATE && SIPRequest.isTargetRefresh(sipRequest.getMethod())) {
			this.doTargetRefresh(sipRequest);
		}

		if(this.dialogState == CONFIRMED_STATE || this.dialogState == TERMINATED_STATE) {
			return;
		}

		// put the contact header from the incoming request into
		// the route set. JvB: some duplication here, ref. doTargetRefresh
		ContactList contactList = sipRequest.getContactHeaders();
		if(contactList != null) {
			this.setRemoteTarget((ContactHeader) contactList.getFirst());
		}

		// Fix for issue #225: mustn't learn Route set from mid-dialog requests
		if(sipRequest.getToTag() != null) {
			return;
		}

		// Incoming Request has the route list
		RecordRouteList rrlist = sipRequest.getRecordRouteHeaders();
		// Add the route set from the incoming response in reverse order
		if(rrlist != null) {
			this.addRoute(rrlist);
		} else {
			// Set the route list to the last seen route list.
			this.routeList = new RouteList();
		}
	}

	/**
	 * Set the dialog identifier.
	 */
	public void setDialogId(String dialogId) {
		if(firstTransaction != null) {
			firstTransaction.setDialog(this, dialogId);
		}

		this.dialogId = dialogId;
	}

	/**
	 * Return true if is server.
	 * 
	 * @return true if is server transaction created this dialog.
	 */
	@Override
	public boolean isServer() {
		if(this.firstTransactionSeen) {
			return this.firstTransactionIsServerTransaction;
		}

		return this.serverTransactionFlag;
	}

	/**
	 * Return true if this is a re-establishment of the dialog.
	 * 
	 * @return true if the reInvite flag is set.
	 */
	protected boolean isReInvite() {
		return this.reInviteFlag;
	}

	/**
	 * Get the id for this dialog.
	 * 
	 * @return the string identifier for this dialog.
	 * 
	 */
	public String getDialogId() {
		if(this.dialogId == null && this.lastResponseDialogId != null) {
			this.dialogId = this.lastResponseDialogId;
		}

		return this.dialogId;
	}

	protected void storeFirstTransactionInfo(SIPDialog dialog, SIPTransaction transaction) {
		dialog.firstTransactionSeen = true;
		dialog.firstTransaction = transaction;
		dialog.firstTransactionIsServerTransaction = transaction.isServerTransaction();

		if(dialog.firstTransactionIsServerTransaction) {
			dialog.firstTransactionSecure = transaction.getRequest().getRequestURI().getScheme()
					.equalsIgnoreCase("sips");
		} else {
			dialog.firstTransactionSecure = ((SIPClientTransaction) transaction).getOriginalRequestScheme()
					.equalsIgnoreCase("sips");
		}

		dialog.firstTransactionPort = transaction.getPort();
		dialog.firstTransactionId = transaction.getBranchId();
		dialog.firstTransactionMethod = transaction.getMethod();

		if(transaction instanceof SIPServerTransaction && dialog.firstTransactionMethod.equals(Request.INVITE)) {
			sipStack.removeMergeDialog(firstTransactionMergeId);

			dialog.firstTransactionMergeId = transaction.getMergeId();

			sipStack.putMergeDialog(this);
		}

		if(transaction.isServerTransaction()) {
			SIPServerTransaction st = (SIPServerTransaction) transaction;

			SIPResponse response = st.getLastResponse();

			dialog.contactHeader = response != null ? response.getContactHeader() : null;
		} else {
			SIPClientTransaction ct = (SIPClientTransaction) transaction;

			dialog.contactHeader = ct.getOriginalRequestContact();
		}

		logger.log(Level.FINEST, "firstTransaction: {0}", dialog.firstTransaction);
		logger.log(Level.FINEST, "firstTransactionIsServerTransaction: {0}", firstTransactionIsServerTransaction);
		logger.log(Level.FINEST, "firstTransactionSecure: {0}", firstTransactionSecure);
		logger.log(Level.FINEST, "firstTransactionPort: {0}", firstTransactionPort);
		logger.log(Level.FINEST, "firstTransactionId: {0}", firstTransactionId);
		logger.log(Level.FINEST, "firstTransactionMethod: {0}", firstTransactionMethod);
		logger.log(Level.FINEST, "firstTransactionMergeId: {0}", firstTransactionMergeId);
	}

	/**
	 * Add a transaction record to the dialog.
	 * 
	 * @param transaction is the transaction to add to the dialog.
	 */
	public boolean addTransaction(SIPTransaction transaction) {
		SIPRequest sipRequest = transaction.getOriginalRequest();

		// Processing a re-invite.
		if(firstTransactionSeen && !firstTransactionId.equals(transaction.getBranchId())
				&& transaction.getMethod().equals(firstTransactionMethod)) {
			setReInviteFlag(true);
		}

		logger.log(Level.FINEST, "SipDialog.addTransaction() {0} transaction: {1}", new Object[] {this, transaction});

		if(firstTransactionSeen == false) {
			// Record the local and remote sequence numbers and the from and to tags for future use on this dialog.
			storeFirstTransactionInfo(this, transaction);

			if(sipRequest.getMethod().equals(Request.SUBSCRIBE)) {
				this.eventHeader = (EventHeader) sipRequest.getHeader(EventHeader.NAME);
			}

			this.setLocalParty(sipRequest);
			this.setRemoteParty(sipRequest);
			this.setCallId(sipRequest);

			if(this.originalRequest == null && transaction.isInviteTransaction()) {
				this.originalRequest = sipRequest;
			} else if(originalRequest != null) {
				originalRequestRecordRouteHeaders = sipRequest.getRecordRouteHeaders();
			}

			if(this.method == null) {
				this.method = sipRequest.getMethod();
			}

			if(transaction instanceof SIPServerTransaction) {
				this.hisTag = sipRequest.getFrom().getTag();
				// My tag is assigned when sending response
			} else {
				setLocalSequenceNumber(sipRequest.getCSeq().getSeqNumber());

				this.originalLocalSequenceNumber = getLocalSeqNumber();
				this.setLocalTag(sipRequest.getFrom().getTag());

				if(myTag == null) {
					logger.log(Level.SEVERE, "The request''s From header is missing the required Tag parameter.");
				}
			}
		} else if(transaction.getMethod().equals(firstTransactionMethod)
				&& firstTransactionIsServerTransaction != transaction.isServerTransaction()) {
			/*
			 * This case occurs when you are processing a re-invite. Switch from client side to server side for
			 * re-invite (put the other side on hold).
			 */

			storeFirstTransactionInfo(this, transaction);

			this.setLocalParty(sipRequest);
			this.setRemoteParty(sipRequest);
			this.setCallId(sipRequest);

			if(transaction.isInviteTransaction()) {
				this.originalRequest = sipRequest;
			} else {
				originalRequestRecordRouteHeaders = sipRequest.getRecordRouteHeaders();
			}

			this.method = sipRequest.getMethod();
		} else if(firstTransaction == null && transaction.isInviteTransaction()) {
			// needed for re-invite reliable processing
			firstTransaction = transaction;
		}

		if(transaction instanceof SIPServerTransaction) {
			setRemoteSequenceNumber(sipRequest.getCSeq().getSeqNumber());
		}

		/*
		 * If this is a server transaction record the remote sequence number to avoid re-processing of requests
		 * with the same sequence number directed towards this dialog.
		 */
		logger.log(Level.FINEST, "isBackToBackUserAgent: {0}", this.isBackToBackUserAgent);

		if(transaction.isInviteTransaction()) {
			logger.log(Level.FINEST, "SIPDialog::setLastTransaction:dialog: {0} lastTransaction: {1}", new Object[] {
					SIPDialog.this, transaction});

			this.lastTransaction = transaction;
		}

		try {
			if(transaction.getRequest().getMethod().equals(Request.REFER)
					&& transaction instanceof SIPServerTransaction) {
				/*
				 * RFC-3515 Section - 2.4.6, if there are multiple REFER transactions in a
				 * dialog then the NOTIFY MUST include an id parameter in the Event header
				 * containing the sequence number (the number from the CSeq header field value)
				 * of the REFER this NOTIFY is associated with. This id parameter MAY be
				 * included in NOTIFYs to the first REFER a UA receives in a given dialog
				 */
				long lastReferCSeq = ((SIPRequest) transaction.getRequest()).getCSeq().getSeqNumber();

				this.eventHeader = new Event();
				this.eventHeader.setEventType("refer");

				logger.log(Level.FINEST, "SIPDialog::setLastTransaction:lastReferCSeq: {0}", lastReferCSeq);

				this.eventHeader.setEventId(Long.toString(lastReferCSeq));
			}
		} catch(Exception ex) {
			logger.log(Level.SEVERE, "Unexpected exception in REFER processing", ex);
		}

		logger.log(Level.FINEST, "Transaction Added: {0} {1}/{2}", new Object[] {this, myTag, hisTag});
		logger.log(Level.FINEST, "TID: {0}/{1}", new Object[] {
				transaction.getTransactionId(), transaction.isServerTransaction()});

		return true;
	}

	/**
	 * Set the remote tag.
	 * 
	 * @param hisTag is the remote tag to set.
	 */
	protected void setRemoteTag(String hisTag) {
		logger.log(Level.FINEST, "setRemoteTag(): {0}, remoteTag: {1}, new tag: {2}", new Object[] {
				this, this.hisTag, hisTag});

		if(this.hisTag != null && hisTag != null && !hisTag.equals(this.hisTag)) {
			if(this.getState() != DialogState.EARLY) {
				logger.log(Level.FINEST, "Dialog is already established -- ignoring remote tag re-assignment");

				return;
			}

			if(sipStack.isRemoteTagReassignmentAllowed()) {
				logger.log(Level.FINEST, "UNSAFE OPERATION !  tag re-assignment {0} trying to set to {1} can cause"
						+ " unexpected effects", new Object[] { this.hisTag, hisTag });

				boolean removed = false;
				if (this.sipStack.getDialog(dialogId) == this) {
					this.sipStack.removeDialog(dialogId);
					removed = true;

				}

				this.dialogId = null;
				this.hisTag = hisTag;

				if(removed) {
					logger.log(Level.FINEST, "ReInserting Dialog");

					this.sipStack.putDialog(this);
				}
			}
		} else {
			if (hisTag != null) {
				this.hisTag = hisTag;
			} else {
				logger.log(Level.WARNING, "setRemoteTag : called with null argument");
			}
		}
	}

	/**
	 * Get the last transaction from the dialog.
	 */
	public SIPTransaction getLastTransaction() {
		return this.lastTransaction;
	}

	/**
	 * Get the INVITE transaction (null if no invite transaction).
	 */
	public SIPServerTransaction getInviteTransaction() {
		DialogTimerTask t = this.timerTask;
		if (t != null)
			return t.transaction;
		else
			return null;
	}

	/**
	 * Set the local sequence number for the dialog (defaults to 1 when the dialog is
	 * created).
	 * 
	 * @param lCseq is the local cseq number.
	 * 
	 */
	private void setLocalSequenceNumber(long lCseq) {
		logger.log(Level.FINEST, "setLocalSequenceNumber: original {0}, new: {1}", new Object[] {
				this.localSequenceNumber, lCseq});

		if(lCseq <= this.localSequenceNumber) {
			throw new IllegalStateException("Sequence number should not decrease!");
		}

		this.localSequenceNumber = lCseq;
	}

	/**
	 * Set the remote sequence number for the dialog.
	 * 
	 * @param rCseq is the remote cseq number.
	 * 
	 */
	public void setRemoteSequenceNumber(long rCseq) {
		logger.log(Level.FINEST, "setRemoteSeqno {0}/{1}", new Object[] {this, rCseq});

		this.remoteSequenceNumber = rCseq;
	}

	/**
	 * Increment the local CSeq # for the dialog. This is useful for if you want to
	 * create a hole in the sequence number i.e. route a request outside the dialog
	 * and then resume within the dialog.
	 */
	@Override
	public void incrementLocalSequenceNumber() {
		++this.localSequenceNumber;
	}

	/**
	 * Get the remote sequence number (for cseq assignment of outgoing requests
	 * within this dialog).
	 * 
	 * @deprecated
	 * @return local sequence number.
	 */
	@Override
	public int getRemoteSequenceNumber() {
		return (int) this.remoteSequenceNumber;
	}

	/**
	 * Get the local sequence number (for cseq assignment of outgoing requests
	 * within this dialog).
	 * 
	 * @deprecated
	 * @return local sequence number.
	 */
	@Override
	public int getLocalSequenceNumber() {
		return (int) this.localSequenceNumber;
	}

	/**
	 * Get the sequence number for the request that origianlly created the Dialog.
	 * 
	 * @return -- the original starting sequence number for this dialog.
	 */
	public long getOriginalLocalSequenceNumber() {
		return this.originalLocalSequenceNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getLocalSequenceNumberLong()
	 */
	@Override
	public long getLocalSeqNumber() {
		return this.localSequenceNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getRemoteSequenceNumberLong()
	 */
	@Override
	public long getRemoteSeqNumber() {
		return this.remoteSequenceNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getLocalTag()
	 */
	public String getLocalTag() {
		return this.myTag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getRemoteTag()
	 */
	@Override
	public String getRemoteTag() {

		return hisTag;
	}

	/**
	 * Set local tag for the transaction.
	 * 
	 * @param mytag is the tag to use in From headers client transactions that
	 *              belong to this dialog and for generating To tags for Server
	 *              transaction requests that belong to this dialog.
	 */
	protected void setLocalTag(String mytag) {
		logger.log(Level.FINEST, "set Local tag {0} dialog {1}", new Object[] {mytag, this});

		this.myTag = mytag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#delete()
	 */
	@Override
	public void delete() {
		// the reaper will get him later.
		this.setState(TERMINATED_STATE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getCallId()
	 */
	@Override
	public CallIdHeader getCallId() {
		// we save the header in a string form and re-parse it, help GC for dialogs updated not too often
		if(callIdHeader == null && callIdHeaderString != null) {
			try {
				this.callIdHeader = (CallIdHeader) new CallIDParser(callIdHeaderString).parse();
			} catch(ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the call id header", e);
			}
		}

		return this.callIdHeader;
	}

	/**
	 * set the call id header for this dialog.
	 */
	private void setCallId(SIPRequest sipRequest) {
		this.callIdHeader = sipRequest.getCallId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getLocalParty()
	 */
	@Override
	public javax.sip.address.Address getLocalParty() {
		// we save the address in a string form and reparse it, help GC for dialogs updated not too often
		if(localParty == null && localPartyStringified != null) {
			try {
				this.localParty = (Address) new AddressParser(localPartyStringified).address(true);
			} catch(ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the localParty", e);
			}
		}

		return this.localParty;
	}

	protected void setLocalParty(SIPMessage sipMessage) {
		if(!isServer()) {
			this.localParty = sipMessage.getFrom().getAddress();
		} else {
			this.localParty = sipMessage.getTo().getAddress();
		}
	}

	/**
	 * Returns the Address identifying the remote party. This is the value of the To
	 * header of locally initiated requests in this dialogue when acting as an User
	 * Agent Client.
	 * <p>
	 * This is the value of the From header of recieved responses in this dialogue
	 * when acting as an User Agent Server.
	 * 
	 * @return the address object of the remote party.
	 */
	public javax.sip.address.Address getRemoteParty() {
		// we save the address in a string form and re-parse it, help GC for dialogs updated not too often
		if(remoteParty == null && remotePartyStringified != null) {
			try {
				this.remoteParty = (Address) new AddressParser(remotePartyStringified).address(true);
			} catch (ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the remoteParty", e);
			}
		}

		logger.log(Level.FINEST, "gettingRemoteParty: {0}", this.remoteParty);

		return this.remoteParty;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getRemoteTarget()
	 */
	@Override
	public javax.sip.address.Address getRemoteTarget() {
		// we save the address in a string form and reparse it, help GC for dialogs updated not too often
		if(remoteTarget == null && remoteTargetStringified != null) {
			try {
				this.remoteTarget = new AddressParser(remoteTargetStringified).address(true);
			} catch(ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the remoteTarget", e);
			}
		}

		return this.remoteTarget;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#getState()
	 */
	@Override
	public DialogState getState() {
		if(this.dialogState == NULL_STATE) {
			// not yet initialized
			return null;
		}

		return DialogState.getObject(this.dialogState);
	}

	/**
	 * Returns true if this Dialog is secure i.e. if the request arrived over TLS,
	 * and the Request-URI contained a SIPS URI, the "secure" flag is set to TRUE.
	 * 
	 * @return <code>true</code> if this dialogue was established using a sips URI
	 *         over TLS, and <code>false</code> otherwise.
	 */
	@Override
	public boolean isSecure() {
		return this.firstTransactionSecure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#sendAck(javax.sip.message.Request)
	 */
	@Override
	public void sendAck(Request request) throws SipException {
		this.sendAck(request, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#createRequest(java.lang.String)
	 */
	@Override
	public Request createRequest(String method) throws SipException {
		if(method.equals(Request.ACK) || method.equals(Request.PRACK)) {
			throw new SipException("Invalid method specified for createRequest:" + method);
		}

		if(lastResponseTopMostVia != null) {
			return this.createRequest(method, this.lastResponseTopMostVia.getTransport());
		}

		throw new SipException("Dialog not yet established -- no response!");
	}

	/**
	 * The method that actually does the work of creating a request.
	 * 
	 * @param method
	 * @param response
	 * @return
	 * @throws SipException
	 */
	private SIPRequest createRequest(String method, String topMostViaTransport) throws SipException {
		/*
		 * Check if the dialog is in the right state (RFC 3261 section 15). The caller's
		 * UA MAY send a BYE for either CONFIRMED or EARLY dialogs, and the callee's UA
		 * MAY send a BYE on CONFIRMED dialogs, but MUST NOT send a BYE on EARLY
		 * dialogs.
		 * 
		 * Throw out cancel request.
		 */

		if(method == null || topMostViaTransport == null) {
			throw new NullPointerException("null argument");
		}

		if(method.equals(Request.CANCEL)) {
			throw new SipException("Dialog.createRequest(): Invalid request");
		}

		if(this.getState() == null
				|| (this.getState().getValue() == TERMINATED_STATE && !method.equalsIgnoreCase(Request.BYE))
				|| (this.isServer() && this.getState().getValue() == EARLY_STATE
						&& method.equalsIgnoreCase(Request.BYE))) {
			throw new SipException("Dialog  " + getDialogId() + " not yet established or terminated "
						+ this.getState());
		}

		SipUri sipUri = null;
		if(this.getRemoteTarget() != null) {
			sipUri = (SipUri) this.getRemoteTarget().getURI().clone();
		} else {
			sipUri = (SipUri) this.getRemoteParty().getURI().clone();
			sipUri.clearUriParms();
		}

		CSeq cseq = new CSeq();
		try {
			cseq.setMethod(method);
			cseq.setSeqNumber(this.getLocalSeqNumber());
		} catch(Exception ex) {
			logger.log(Level.SEVERE, "Unexpected error");

			InternalErrorHandler.handleException(ex);
		}

		/*
		 * Add a via header for the outbound request based on the transport of the
		 * message processor.
		 */
		ListeningPointImpl lp = (ListeningPointImpl) this.sipProvider.getListeningPoint(topMostViaTransport);

		if(lp == null) {
			logger.log(Level.SEVERE, "Cannot find listening point for transport: {0}", topMostViaTransport);

			throw new SipException("Cannot find listening point for transport " + topMostViaTransport);
		}

		Via via = lp.getViaHeader();
		From from = new From();

		from.setAddress(this.getLocalParty());

		To to = new To();

		to.setAddress(this.getRemoteParty());

		SIPRequest sipRequest = createRequest(sipUri, via, cseq, from, to);

		/*
		 * The default contact header is obtained from the provider. The application can
		 * override this.
		 * 
		 * JvB: Should only do this for target refresh requests, ie not for BYE, PRACK,
		 * etc
		 */

		if(SIPRequest.isTargetRefresh(method)) {
			ContactHeader contactHeader = ((ListeningPointImpl) this.sipProvider.getListeningPoint(lp.getTransport()))
					.createContactHeader();

			((SipURI) contactHeader.getAddress().getURI()).setSecure(this.isSecure());

			sipRequest.setHeader(contactHeader);
		}

		try {
			/*
			 * Guess of local sequence number - this is being re-set when the request is
			 * actually dispatched
			 */
			cseq = (CSeq) sipRequest.getCSeq();
			cseq.setSeqNumber(getLocalSeqNumber() + 1);

			logger.log(Level.FINEST, "SIPDialog::createRequest:setting Request Seq Number to {0}", cseq.getSeqNumber());
		} catch (InvalidArgumentException ex) {
			InternalErrorHandler.handleException(ex);
		}

		if(method.equals(Request.SUBSCRIBE) && eventHeader != null) {
			sipRequest.addHeader(eventHeader);
		}

		/*
		 * RFC-3515 Section - 2.4.6, if there are multiple REFER transactions in a
		 * dialog then the NOTIFY MUST include an id parameter in the Event header
		 * containing the sequence number (the number from the CSeq header field value)
		 * of the REFER this NOTIFY is associated with. This id parameter MAY be
		 * included in NOTIFYs to the first REFER a UA receives in a given dialog
		 */
		if(method.equals(Request.NOTIFY) && eventHeader != null) {
			sipRequest.addHeader(eventHeader);
		}

		/*
		 * RFC3261, section 12.2.1.1:
		 * 
		 * The URI in the To field of the request MUST be set to the remote URI from the
		 * dialog state. The tag in the To header field of the request MUST be set to
		 * the remote tag of the dialog ID. The From URI of the request MUST be set to
		 * the local URI from the dialog state. The tag in the From header field of the
		 * request MUST be set to the local tag of the dialog ID. If the value of the
		 * remote or local tags is null, the tag parameter MUST be omitted from the To
		 * or From header fields, respectively.
		 */
		try {
			if(this.getLocalTag() != null) {
				from.setTag(this.getLocalTag());
			} else {
				from.removeTag();
			}

			if(this.getRemoteTag() != null) {
				to.setTag(this.getRemoteTag());
			} else {
				to.removeTag();
			}
		} catch(ParseException ex) {
			InternalErrorHandler.handleException(ex);
		}

		// get the route list from the dialog.
		this.updateRequest(sipRequest);

		return sipRequest;
	}

	/**
	 * Generate a request from a response.
	 * 
	 * @param requestURI -- the request URI to assign to the request.
	 * @param via        -- the Via header to assign to the request
	 * @param cseq       -- the CSeq header to assign to the request
	 * @param from       -- the From header to assign to the request
	 * @param to         -- the To header to assign to the request
	 * @return -- the newly generated sip request.
	 */
	public SIPRequest createRequest(SipUri requestURI, Via via, CSeq cseq, From from, To to) {
		SIPRequest newRequest = new SIPRequest();
		String method = cseq.getMethod();

		newRequest.setMethod(method);
		newRequest.setRequestURI(requestURI);
		this.setBranch(via, method);
		newRequest.setHeader(via);
		newRequest.setHeader(cseq);
		newRequest.setHeader(from);
		newRequest.setHeader(to);
		newRequest.setHeader(getCallId());

		try {
			// all requests need a Max-Forwards
			newRequest.attachHeader(new MaxForwards(70), false);
		} catch(Exception d) {
			logger.log(Level.FINEST, "silently ignoring exception", d);
		}

		if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
			newRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
		}

		return newRequest;
	}

	/**
	 * Sets the Via branch for CANCEL or ACK requests
	 * 
	 * @param via
	 * @param method
	 * @throws ParseException
	 */
	private final void setBranch(Via via, String method) {
		String branch;
		if (method.equals(Request.ACK)) {
			if (getLastResponseStatusCode().intValue() >= 300) {
				branch = lastResponseTopMostVia.getBranch(); // non-2xx ACK uses
				// same branch
			} else {
				branch = Utils.getInstance().generateBranchId(); // 2xx ACK gets
				// new branch
			}
		} else if (method.equals(Request.CANCEL)) {
			branch = lastResponseTopMostVia.getBranch(); // CANCEL uses same
			// branch
		} else
			return;

		try {
			via.setBranch(branch);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#sendRequest(javax.sip.ClientTransaction)
	 */
	@Override
	public void sendRequest(ClientTransaction clientTransactionId) throws SipException {
		this.sendRequest(clientTransactionId, !this.isBackToBackUserAgent);
	}

	public void sendRequest(ClientTransaction clientTransaction, boolean allowInterleaving) throws SipException {
		if(null == clientTransaction) {
			throw new NullPointerException("null parameter");
		}

		if((!allowInterleaving) && clientTransaction.getRequest().getMethod().equals(Request.INVITE)) {
			logger.log(Level.FINEST, "SIPDialog::sendRequest {0}, clientTransaction: {1}",
					new Object[] {this, clientTransaction});

			sipStack.getReinviteExecutor().execute((new ReInviteSender(clientTransaction)));

			return;
		}

		SIPRequest dialogRequest = ((SIPClientTransaction) clientTransaction).getOriginalRequest();

		this.proxyAuthorizationHeader = (ProxyAuthorizationHeader) dialogRequest
				.getHeader(ProxyAuthorizationHeader.NAME);

		logger.log(Level.FINEST, "SIPDialog::sendRequest:dialog.sendRequest dialog: {0}\ndialogRequest:\n{1}",
					new Object[] { this, dialogRequest });

		if(dialogRequest.getMethod().equals(Request.ACK) || dialogRequest.getMethod().equals(Request.CANCEL)) {
			throw new SipException("Bad Request Method. " + dialogRequest.getMethod());
		}

		// added, allow re-sending of BYE after challenge
		if(byeSent && isTerminatedOnBye() && !dialogRequest.getMethod().equals(Request.BYE)) {
			logger.log(Level.SEVERE, "BYE already sent for: {0}", this);

			throw new SipException("Cannot send request; BYE already sent");
		}

		if(dialogRequest.getTopmostVia() == null) {
			dialogRequest.addHeader(((SIPClientTransaction) clientTransaction).getOutgoingViaHeader());
		}

		if(!this.getCallId().getCallId().equalsIgnoreCase(dialogRequest.getCallId().getCallId())) {
			logger.log(Level.SEVERE, "CallID: {0}", this.getCallId());
			logger.log(Level.SEVERE, "SIPDialog::sendRequest:RequestCallID: {0}",
					dialogRequest.getCallId().getCallId());
			logger.log(Level.SEVERE, "dialog: {0}", this);

			throw new SipException("Bad call ID in request");
		}

		// Set the dialog back pointer.
		((SIPClientTransaction) clientTransaction).setDialog(this, this.dialogId);

		this.addTransaction((SIPTransaction) clientTransaction);
		// Enable the retransmission filter for the transaction

		((SIPClientTransaction) clientTransaction).setTransactionMapped(true);

		From from = (From) dialogRequest.getFrom();
		To to = (To) dialogRequest.getTo();

		// Caller already did the tag assignment -- check to see if the tag assignment is OK.
		if(this.getLocalTag() != null && from.getTag() != null && !from.getTag().equals(this.getLocalTag())) {
			throw new SipException("From tag mismatch expecting  " + this.getLocalTag());
		}

		if(this.getRemoteTag() != null && to.getTag() != null && !to.getTag().equals(this.getRemoteTag())) {
			logger.log(Level.WARNING, "SIPDialog::sendRequest:To header tag mismatch expecting: {0}",
					this.getRemoteTag());
		}

		/*
		 * The application is sending a NOTIFY before sending the response of the dialog.
		 */
		if(this.getLocalTag() == null && dialogRequest.getMethod().equals(Request.NOTIFY)) {
			if(!this.getMethod().equals(Request.SUBSCRIBE)) {
				throw new SipException("Trying to send NOTIFY without SUBSCRIBE Dialog!");
			}

			this.setLocalTag(from.getTag());
		}

		try {
			if(this.getLocalTag() != null) {
				from.setTag(this.getLocalTag());
			}

			if(this.getRemoteTag() != null) {
				to.setTag(this.getRemoteTag());
			}
		} catch(ParseException ex) {
			InternalErrorHandler.handleException(ex);
		}

		Hop hop = ((SIPClientTransaction) clientTransaction).getNextHop();

		logger.log(Level.FINEST, "SIPDialog::sendRequest:Using hop: {0}:{1}",
				new Object[] {hop.getHost(), hop.getPort()});

		try {
			MessageChannel messageChannel = sipStack.createRawMessageChannel(
					this.getSipProvider().getListeningPoint(hop.getTransport()).getIPAddress(),
					this.firstTransactionPort, hop);

			MessageChannel oldChannel = ((SIPClientTransaction) clientTransaction).getMessageChannel();

			// Remove this from the connection cache if it is in the connection cache and is not yet active.
			oldChannel.uncache();

			// Not configured to cache client connections.
			if (!sipStack.cacheClientConnections) {
				oldChannel.useCount--;

				logger.log(Level.FINEST, "SIPDialog::sendRequest:oldChannel: useCount: {0}", oldChannel.useCount);
			}

			if(messageChannel == null) {
				/*
				 * At this point the procedures of 8.1.2 and 12.2.1.1 of RFC3261 have been tried
				 * but the resulting next hop cannot be resolved (recall that the exception
				 * thrown is caught and ignored in SIPStack.createMessageChannel() so we end up
				 * here with a null messageChannel instead of the exception handler below). All
				 * else failing, try the outbound proxy in accordance with 8.1.2, in particular:
				 * This ensures that outbound proxies that do not add Record-Route header field
				 * values will drop out of the path of subsequent requests. It allows endpoints
				 * that cannot resolve the first Route URI to delegate that task to an outbound
				 * proxy.
				 * 
				 * if one considers the 'first Route URI' of a request constructed according to
				 * 12.2.1.1 to be the request URI when the route set is empty.
				 */
				logger.log(Level.FINEST, "Null message channel using outbound proxy!");

				Hop outboundProxy = sipStack.getRouter(dialogRequest).getOutboundProxy();
				if(outboundProxy == null) {
					throw new SipException("No route found! hop=" + hop);
				}

				messageChannel = sipStack.createRawMessageChannel(
						this.getSipProvider().getListeningPoint(outboundProxy.getTransport()).getIPAddress(),
						this.firstTransactionPort, outboundProxy);

				if(messageChannel != null) {
					((SIPClientTransaction) clientTransaction).setEncapsulatedChannel(messageChannel);
				}
			} else {
				((SIPClientTransaction) clientTransaction).setEncapsulatedChannel(messageChannel);

				logger.log(Level.FINEST, "SIPDialog::sendRequest:using message channel: {0}", messageChannel);
			}

			if(messageChannel != null) {
				messageChannel.useCount++;
			}

			// See if we need to release the previously mapped channel.
			if((!sipStack.cacheClientConnections) && oldChannel != null && oldChannel.useCount <= 0) {
				oldChannel.close();
			}
		} catch(Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);

			throw new SipException("Could not create message channel", ex);
		}

		try {
			// Increment before setting!!
			long cseqNumber = dialogRequest.getCSeq() == null ? getLocalSeqNumber() : dialogRequest.getCSeq()
					.getSeqNumber();

			if(cseqNumber > getLocalSeqNumber()) {
				setLocalSequenceNumber(cseqNumber);
			} else {
				setLocalSequenceNumber(getLocalSeqNumber() + 1);
			}

			logger.log(Level.FINEST, "SIPDialog::sendRequest:setting Seq Number to {0}", getLocalSeqNumber());

			dialogRequest.getCSeq().setSeqNumber(getLocalSeqNumber());
		} catch(InvalidArgumentException ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}

		try {
			((SIPClientTransaction) clientTransaction).sendMessage(dialogRequest);

			/*
			 * Note that if the BYE is rejected then the Dialog should bo back to the
			 * ESTABLISHED state so we only set state after successful send.
			 */
			if(dialogRequest.getMethod().equals(Request.BYE)) {
				this.byeSent = true;

				/*
				 * Dialog goes into TERMINATED state as soon as BYE is sent. ISSUE 182.
				 */
				if(isTerminatedOnBye()) {
					this.setState(DialogState.TERMINATED_VALUE);
				}
			}
		} catch(IOException ex) {
			throw new SipException("error sending message", ex);
		}
	}

	/**
	 * Return yes if the last response is to be retransmitted.
	 */
	private boolean toRetransmitFinalResponse(int t2) {
		if(--retransmissionTicksLeft == 0) {
			if (2 * prevRetransmissionTicks <= t2) {
				this.retransmissionTicksLeft = 2 * prevRetransmissionTicks;
			} else {
				this.retransmissionTicksLeft = prevRetransmissionTicks;
			}

			this.prevRetransmissionTicks = retransmissionTicksLeft;

			return true;
		}

		return false;
	}

	protected void setRetransmissionTicks() {
		this.retransmissionTicksLeft = 1;
		this.prevRetransmissionTicks = 1;
	}

	/**
	 * Resend the last ACK.
	 */
	public void resendAck() throws SipException {
		// Check for null.

		if (this.getLastAckSent() != null) {
			if (getLastAckSent().getHeader(TimeStampHeader.NAME) != null && sipStack.generateTimeStampHeader) {
				TimeStamp ts = new TimeStamp();
				try {
					ts.setTimeStamp(System.currentTimeMillis());
					getLastAckSent().setHeader(ts);
				} catch (InvalidArgumentException e) {

				}
			}
			this.sendAck(getLastAckSent(), false);
		} else {
			logger.log(Level.FINEST, "SIPDialog::resendAck:lastAck sent is NULL hence not resending ACK");
		}
	}

	/**
	 * Get the method of the request/response that resulted in the creation of the
	 * Dialog.
	 * 
	 * @return -- the method of the dialog.
	 */
	public String getMethod() {
		// Method of the request or response used to create this dialog
		return this.method;
	}

	/**
	 * Start the dialog timer.
	 * 
	 * @param transaction
	 */
	protected void startTimer(SIPServerTransaction transaction) {
		if(this.timerTask != null && timerTask.transaction == transaction) {
			logger.log(Level.FINEST, "Timer already running for: {0}", getDialogId());

			return;
		}

		logger.log(Level.FINEST, "Starting dialog timer for: {0}", getDialogId());

		acquireTimerTaskSem();

		try {
			if(this.timerTask != null) {
				this.timerTask.transaction = transaction;
			} else {
				this.timerTask = new DialogTimerTask(transaction);

				if(sipStack.getTimer() != null && sipStack.getTimer().isStarted()) {
					sipStack.getTimer().scheduleWithFixedDelay(timerTask, transaction.getBaseTimerInterval(),
							transaction.getBaseTimerInterval());
				}
			}
		} finally {
			releaseTimerTaskSem();
		}

		this.setRetransmissionTicks();
	}

	/**
	 * Stop the dialog timer. This is called when the dialog is terminated.
	 */
	protected void stopTimer() {
		try {
			acquireTimerTaskSem();
			try {
				if(this.timerTask != null) {
					this.getStack().getTimer().cancel(timerTask);
					this.timerTask = null;
				}

				if(this.earlyStateTimerTask != null) {
					this.getStack().getTimer().cancel(this.earlyStateTimerTask);
					this.earlyStateTimerTask = null;
				}
			} finally {
				releaseTimerTaskSem();
			}
		} catch (Exception ex) {
			logger.log(Level.FINEST, "silently ignoring exception", ex);
		}
	}

	/*
	 * (non-Javadoc) Retransmissions of the reliable provisional response cease when
	 * a matching PRACK is received by the UA core. PRACK is like any other request
	 * within a dialog, and the UAS core processes it according to the procedures of
	 * Sections 8.2 and 12.2.2 of RFC 3261. A matching PRACK is defined as one
	 * within the same dialog as the response, and whose method, CSeq-num, and
	 * response-num in the RAck header field match, respectively, the method from
	 * the CSeq, the sequence number from the CSeq, and the sequence number from the
	 * RSeq of the reliable provisional response.
	 * 
	 * @see javax.sip.Dialog#createPrack(javax.sip.message.Response)
	 */
	public Request createPrack(Response relResponse) throws SipException {
		if(this.getState() == null || this.getState().equals(DialogState.TERMINATED)) {
			throw new DialogDoesNotExistException("Dialog not initialized or terminated");
		}

		if((RSeq) relResponse.getHeader(RSeqHeader.NAME) == null) {
			throw new SipException("Missing RSeq Header");
		}

		try {
			SIPResponse sipResponse = (SIPResponse) relResponse;
			SIPRequest sipRequest = this.createRequest(Request.PRACK, sipResponse.getTopmostVia().getTransport());
			String toHeaderTag = sipResponse.getTo().getTag();

			sipRequest.setToTag(toHeaderTag);

			RAck rack = new RAck();
			RSeq rseq = (RSeq) relResponse.getHeader(RSeqHeader.NAME);

			rack.setMethod(sipResponse.getCSeq().getMethod());
			rack.setCSequenceNumber((int) sipResponse.getCSeq().getSeqNumber());
			rack.setRSequenceNumber(rseq.getSeqNumber());

			sipRequest.setHeader(rack);

			if(this.proxyAuthorizationHeader != null) {
				sipRequest.addHeader(proxyAuthorizationHeader);
			}

			return sipRequest;
		} catch(Exception ex) {
			InternalErrorHandler.handleException(ex);

			return null;
		}
	}

	private void updateRequest(SIPRequest sipRequest) {
		RouteList rl = this.getRouteList();

		if(rl.size() > 0) {
			sipRequest.setHeader(rl);
		} else {
			sipRequest.removeHeader(RouteHeader.NAME);
		}

		if(MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
			sipRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
		}

		/*
		 * Update the request with Proxy auth header if one has been cached.
		 */
		if(this.proxyAuthorizationHeader != null && sipRequest.getHeader(ProxyAuthorizationHeader.NAME) == null) {
			sipRequest.setHeader(proxyAuthorizationHeader);
		}
	}

	/*
	 * (non-Javadoc) The UAC core MUST generate an ACK request for each 2xx received
	 * from the transaction layer. The header fields of the ACK are constructed in
	 * the same way as for any request sent within a dialog (see Section 12) with
	 * the exception of the CSeq and the header fields related to authentication.
	 * The sequence number of the CSeq header field MUST be the same as the INVITE
	 * being acknowledged, but the CSeq method MUST be ACK. The ACK MUST contain the
	 * same credentials as the INVITE. If the 2xx contains an offer (based on the
	 * rules above), the ACK MUST carry an answer in its body. If the offer in the
	 * 2xx response is not acceptable, the UAC core MUST generate a valid answer in
	 * the ACK and then send a BYE immediately.
	 * 
	 * Note that for the case of forked requests, you can create multiple outgoing
	 * invites each with a different cseq and hence you need to supply the invite.
	 * 
	 * @see javax.sip.Dialog#createAck(long)
	 */
	@Override
	public Request createAck(long cseqno) throws InvalidArgumentException, SipException {
		// strictly speaking it is allowed to start a dialog with SUBSCRIBE, then send INVITE+ACK later on
		if(!method.equals(Request.INVITE)) {
			throw new SipException("Dialog was not created with an INVITE" + method);
		}

		if(cseqno <= 0) {
			throw new InvalidArgumentException("bad cseq <= 0 ");
		}

		if(cseqno > ((((long) 1) << 32) - 1)) {
			throw new InvalidArgumentException("bad cseq > " + ((((long) 1) << 32) - 1));
		}

		if(this.getRemoteTarget() == null) {
			throw new SipException("Cannot create ACK - no remote Target!");
		}

		logger.log(Level.FINEST, "createAck {0} cseqno {1}", new Object[] {this, cseqno});

		/*
		 * MUST ACK in the same order that the OKs were received. This traps out of order ACK sending. Old ACKs
		 * seqno's can always be ACKed.
		 */
		if(lastInviteOkReceived < cseqno) {
			logger.log(Level.FINEST, "WARNING : Attempt to crete ACK without OK: {0}", this);
			logger.log(Level.FINEST, "LAST RESPONSE: {0}", this.getLastResponseStatusCode());

			throw new SipException("Dialog not yet established -- no OK response! lastInviteOkReceived="
					+ lastInviteOkReceived + " cseqno=" + cseqno);
		}

		try {
			/*
			 * Transport from first entry in route set, or remote Contact if none Only used to find correct LP
			 * & create correct Via
			 */
			SipURI uri4transport = null;

			if(this.routeList != null && !this.routeList.isEmpty()) {
				Route r = (Route) this.routeList.getFirst();

				uri4transport = ((SipURI) r.getAddress().getURI());
			} else {
				// should be !=null, checked above
				uri4transport = ((SipURI) this.getRemoteTarget().getURI());
			}

			String transport = uri4transport.getTransportParam();
			ListeningPointImpl lp;

			if(uri4transport.isSecure()) {
				// Fix for https://java.net/jira/browse/JSIP-492
				if(transport != null && transport.equalsIgnoreCase(ListeningPoint.UDP)) {
					throw new SipException("Cannot create ACK - impossible to use sips uri with transport UDP:"
							+ uri4transport);
				}

				transport = ListeningPoint.TLS;
			}

			if(transport != null) {
				lp = (ListeningPointImpl) sipProvider.getListeningPoint(transport);
			} else {
				// also support TLS
				if(uri4transport.isSecure()) {
					lp = (ListeningPointImpl) sipProvider.getListeningPoint(ListeningPoint.TLS);
				} else {
					lp = (ListeningPointImpl) sipProvider.getListeningPoint(ListeningPoint.UDP);

					// let's try to find TCP
					if(lp == null) { 
						lp = (ListeningPointImpl) sipProvider.getListeningPoint(ListeningPoint.TCP);
					}
				}
			}

			logger.log(Level.FINEST, "uri4transport: {0}", uri4transport);

			if(lp == null) {
				if(!uri4transport.isSecure()) {
					// If transport is not secure, and we cannot find an appropriate transport, try
					// any supported transport to send out the ACK.
					logger.log(Level.FINEST, "No Listening point for {0} Using last response topmost", uri4transport);

					// We are not on a secure connection and we don't support the transport required
					lp = (ListeningPointImpl) sipProvider.getListeningPoint(this.lastResponseTopMostVia.getTransport());
				}

				if(lp == null) {
					logger.log(Level.SEVERE, "remoteTargetURI: {0}", this.getRemoteTarget().getURI());
					logger.log(Level.SEVERE, "uri4transport: {0}", uri4transport);
					logger.log(Level.SEVERE, "No LP found for transport: {0}", transport);

					throw new SipException("Cannot create ACK - no ListeningPoint for transport towards next hop"
							+ " found:" + transport);
				}
			}

			SIPRequest sipRequest = new SIPRequest();

			sipRequest.setMethod(Request.ACK);
			sipRequest.setRequestURI((SipUri) getRemoteTarget().getURI().clone());
			sipRequest.setCallId(this.getCallId());
			sipRequest.setCSeq(new CSeq(cseqno, Request.ACK));

			List<Via> vias = new ArrayList<>();

			/*
			 * The user may have touched the sent by for the response. so use the via header extracted from the
			 * response for the ACK => https://jain-sip.dev.java.net/issues/show_bug.cgi?id=205 strip the params
			 * from the via of the response and use the params from the original request
			 */
			Via via = this.lastResponseTopMostVia;

			logger.log(Level.FINEST, "lastResponseTopMostVia: {0}", lastResponseTopMostVia);

			via.removeParameters();
			if(originalRequest != null && originalRequest.getTopmostVia() != null) {
				NameValueList originalRequestParameters = originalRequest.getTopmostVia().getParameters();

				if(originalRequestParameters != null && originalRequestParameters.size() > 0) {
					via.setParameters((NameValueList) originalRequestParameters.clone());
				}
			}

			// new branch
			via.setBranch(Utils.getInstance().generateBranchId());
			vias.add(via);

			logger.log(Level.FINEST, "Adding via to the ACK we are creating: {0}, lastResponseTopMostVia: {1}",
					new Object[] { via, lastResponseTopMostVia });

			sipRequest.setVia(vias);

			From from = new From();

			from.setAddress(this.getLocalParty());
			from.setTag(this.myTag);

			sipRequest.setFrom(from);

			To to = new To();

			to.setAddress(this.getRemoteParty());

			if(hisTag != null) {
				to.setTag(this.hisTag);
			}

			sipRequest.setTo(to);
			sipRequest.setMaxForwards(new MaxForwards(70));

			if(this.originalRequest != null) {
				Authorization authorization = this.originalRequest.getAuthorization();

				if(authorization != null) {
					sipRequest.setHeader(authorization);
				}

				/*
				 * setting back the original Request to null to avoid keeping references around for too long
				 * since it is used only in the dialog setup
				 */
				originalRequestRecordRouteHeaders = originalRequest.getRecordRouteHeaders();
				originalRequest = null;
			}

			/*
			 * ACKs for 2xx responses use the Route values learned from the Record-Route of the 2xx responses.
			 */
			this.updateRequest(sipRequest);

			return sipRequest;
		} catch(Exception ex) {
			InternalErrorHandler.handleException(ex);

			throw new SipException("unexpected exception ", ex);
		}
	}

	/**
	 * Get the provider for this Dialog.
	 * 
	 * SPEC_REVISION
	 * 
	 * @return -- the SIP Provider associated with this transaction.
	 */
	@Override
	public SipProviderImpl getSipProvider() {
		return this.sipProvider;
	}

	/**
	 * @param sipProvider the sipProvider to set
	 */
	public void setSipProvider(SipProviderImpl sipProvider) {
		this.sipProvider = sipProvider;
	}

	/**
	 * Check the tags of the response against the tags of the Dialog. Return true if
	 * the respnnse matches the tags of the dialog. We do this check wehn sending out
	 * a response.
	 * 
	 * @param sipResponse -- the response to check.
	 * 
	 */
	public void setResponseTags(SIPResponse sipResponse) {
		if(this.getLocalTag() != null || this.getRemoteTag() != null) {
			return;
		}

		String responseFromTag = sipResponse.getFromTag();
		if(responseFromTag != null) {
			if(responseFromTag.equals(this.getLocalTag())) {
				sipResponse.setToTag(this.getRemoteTag());
			} else if(responseFromTag.equals(this.getRemoteTag())) {
				sipResponse.setToTag(this.getLocalTag());
			}
		} else {
			logger.log(Level.WARNING, "No from tag in response! Not RFC 3261 compatible.");
		}
	}

	/**
	 * Set the last response for this dialog. This method is called for updating the
	 * dialog state when a response is either sent or received from within a Dialog.
	 * 
	 * @param transaction -- the transaction associated with the response
	 * @param sipResponse -- the last response to set.
	 */
	public void setLastResponse(SIPTransaction transaction, SIPResponse sipResponse) {
		this.callIdHeader = sipResponse.getCallId();
		final int statusCode = sipResponse.getStatusCode();

		if(statusCode == 100) {
			logger.log(Level.FINEST, "Invalid status code - 100 in setLastResponse - ignoring");

			return;
		}

		try {
			this.lastResponseStatusCode = Integer.valueOf(statusCode);

			// Issue 378 : http://java.net/jira/browse/JSIP-378
			// Cloning the via header to avoid race condition and be modified
			this.lastResponseTopMostVia = (Via) sipResponse.getTopmostVia().clone();

			String cseqMethod = sipResponse.getCSeqHeader().getMethod();

			this.lastResponseMethod = cseqMethod;

			long responseCSeqNumber = sipResponse.getCSeq().getSeqNumber();

			this.lastResponseCSeqNumber = responseCSeqNumber;

			if(Request.INVITE.equals(cseqMethod)) {
				this.lastInviteResponseCSeqNumber = responseCSeqNumber;
				this.lastInviteResponseCode = statusCode;
			}

			if(sipResponse.getToTag() != null) {
				this.lastResponseToTag = sipResponse.getToTag();
			}

			if(sipResponse.getFromTag() != null) {
				this.lastResponseFromTag = sipResponse.getFromTag();
			}

			if(transaction != null) {
				this.lastResponseDialogId = sipResponse.getDialogId(transaction.isServerTransaction());
			}

			this.setAssigned();

			// Adjust state of the Dialog state machine.
			logger.log(Level.FINEST, "sipDialog: setLastResponse: {0}, lastResponse: {1}, response: {2},"
					+ " topMostViaHeader: {3}", new Object[] {this, this.lastResponseStatusCode, sipResponse,
							lastResponseTopMostVia});

			if(this.getState() == DialogState.TERMINATED) {
				logger.log(Level.FINEST, "sipDialog: setLastResponse -- dialog is terminated - ignoring");

				/*
				 * Capture the OK response for later use in createAck This is handy for late arriving OK's that
				 * we want to ACK.
				 */
				if(cseqMethod.equals(Request.INVITE) && statusCode == 200) {
					this.lastInviteOkReceived = Math.max(responseCSeqNumber, this.lastInviteOkReceived);
				}

				return;
			}

			logger.log(Level.FINEST, "cseqMethod: {0}", cseqMethod);
			logger.log(Level.FINEST, "dialogState: {0}", this.getState());
			logger.log(Level.FINEST, "method: {0}", this.getMethod());
			logger.log(Level.FINEST, "statusCode: {0}", statusCode);
			logger.log(Level.FINEST, "transaction: {0}", transaction);

			/*
			 * don't use "!this.isServer" here note that the transaction can be null for forked responses.
			 */
			if(transaction == null || transaction instanceof ClientTransaction) {
				if(SIPTransactionStack.isDialogCreated(cseqMethod)) {
					// Make a final tag assignment.
					if(getState() == null && (statusCode / 100 == 1)) {
						/*
						 * Guard aginst slipping back into early state from confirmed state.
						 */

						setState(SIPDialog.EARLY_STATE);

						if((sipResponse.getToTag() != null || sipStack.rfc2543Supported) && this.getRemoteTag() == null) {
							setRemoteTag(sipResponse.getToTag());

							this.setDialogId(sipResponse.getDialogId(false));

							sipStack.putDialog(this);

							this.addRoute(sipResponse);
						}
					} else if(getState() != null && getState().equals(DialogState.EARLY) && statusCode / 100 == 1) {
						/*
						 * This case occurs for forked dialog responses. The To tag can change as a
						 * result of the forking. The remote target can also change as a result of the
						 * forking.
						 */
						if(cseqMethod.equals(getMethod()) && transaction != null
								&& (sipResponse.getToTag() != null || sipStack.rfc2543Supported)) {
							setRemoteTag(sipResponse.getToTag());
							this.setDialogId(sipResponse.getDialogId(false));

							sipStack.putDialog(this);

							this.addRoute(sipResponse);
						}
					} else if(statusCode / 100 == 2) {
						// This is a dialog creating method (such as INVITE).
						// 2xx response -- set the state to the confirmed
						// state. To tag is MANDATORY for the response.

						// Only do this if method equals initial request!
						logger.log(Level.FINEST, "pendingRouteUpdateOn202Response: {0}",
								this.pendingRouteUpdateOn202Response);

						if (cseqMethod.equals(getMethod())
								&& (sipResponse.getToTag() != null || sipStack.rfc2543Supported)
								&& (this.getState() != DialogState.CONFIRMED
										|| (this.getState() == DialogState.CONFIRMED
												&& cseqMethod.equals(Request.SUBSCRIBE)
												&& this.pendingRouteUpdateOn202Response
												&& sipResponse.getStatusCode() == Response.ACCEPTED))) {
							if(this.getState() != DialogState.CONFIRMED) {
								setRemoteTag(sipResponse.getToTag());

								this.setDialogId(sipResponse.getDialogId(false));

								sipStack.putDialog(this);

								this.addRoute(sipResponse);
								this.setState(CONFIRMED_STATE);
							}

							/*
							 * Note: Subscribe NOTIFY processing. The route set is computed only after we
							 * get the 202 response but the NOTIFY may come in before we get the 202
							 * response. So we need to update the route set after we see the 202 despite the
							 * fact that the dialog is in the CONFIRMED state. We do this only on the dialog
							 * forming SUBSCRIBE an not a re-subscribe.
							 */
							if(cseqMethod.equals(Request.SUBSCRIBE) && sipResponse.getStatusCode() == Response.ACCEPTED
									&& this.pendingRouteUpdateOn202Response) {
								setRemoteTag(sipResponse.getToTag());

								this.addRoute(sipResponse);
								this.pendingRouteUpdateOn202Response = false;
							}
						}

						// Capture the OK response for later use in createAck
						if(cseqMethod.equals(Request.INVITE)) {
							this.lastInviteOkReceived = Math.max(responseCSeqNumber, this.lastInviteOkReceived);

							if(getState() != null && getState().getValue() == SIPDialog.CONFIRMED_STATE
									&& transaction != null) {
								// http://java.net/jira/browse/JSIP-444 Honor Target Refresh on Response
								doTargetRefresh(sipResponse);
							}
						}
					} else if(statusCode >= 300 && statusCode <= 699
							&& (getState() == null
							|| (cseqMethod.equals(getMethod()) && getState().getValue() == SIPDialog.EARLY_STATE))) {
						/*
						 * This case handles 3xx, 4xx, 5xx and 6xx responses. RFC 3261 Section 12.3 -
						 * dialog termination. Independent of the method, if a request outside of a
						 * dialog generates a non-2xx final response, any early dialogs created through
						 * provisional responses to that request are terminated.
						 */
						setState(SIPDialog.TERMINATED_STATE);
					}

					/*
					 * This code is in support of "proxy" servers that are constructed as back to
					 * back user agents. This could be a dialog in the middle of the call setup path
					 * somewhere. Hence the incoming invite has record route headers in it. The
					 * response will have additional record route headers. However, for this dialog
					 * only the downstream record route headers matter. Ideally proxy servers should
					 * not be constructed as Back to Back User Agents. Remove all the record routes
					 * that are present in the incoming INVITE so you only have the downstream Route
					 * headers present in the dialog. Note that for an endpoint - you will have no
					 * record route headers present in the original request so the loop will not
					 * execute.
					 */
					if(this.getState() != DialogState.CONFIRMED && this.getState() != DialogState.TERMINATED
							&& getOriginalRequestRecordRouteHeaders() != null) {
						ListIterator<RecordRoute> it = getOriginalRequestRecordRouteHeaders()
								.listIterator(getOriginalRequestRecordRouteHeaders().size());

						while(it.hasPrevious()) {
							RecordRoute rr = it.previous();
							Route route = (Route) routeList.getFirst();

							if(route != null && rr.getAddress().equals(route.getAddress())) {
								routeList.removeFirst();
							} else {
								break;
							}
						}
					}
				} else if(cseqMethod.equals(Request.NOTIFY)
						&& (this.getMethod().equals(Request.SUBSCRIBE) || this.getMethod().equals(Request.REFER))
						&& sipResponse.getStatusCode() / 100 == 2 && this.getState() == null) {
					// This is a notify response.
					this.setDialogId(sipResponse.getDialogId(true));

					sipStack.putDialog(this);

					this.setState(SIPDialog.CONFIRMED_STATE);
				} else if(cseqMethod.equals(Request.BYE) && statusCode / 100 == 2 && isTerminatedOnBye()) {
					// Dialog will be terminated when the transaction is terminated.
					setState(SIPDialog.TERMINATED_STATE);
				}
			} else {
				// Processing Server Dialog.
				if(cseqMethod.equals(Request.BYE) && statusCode / 100 == 2 && this.isTerminatedOnBye()) {
					/*
					 * Only transition to terminated state when 200 OK is returned for the BYE.
					 * Other status codes just result in leaving the state in COMPLETED state.
					 */
					this.setState(SIPDialog.TERMINATED_STATE);
				} else {
					boolean doPutDialog = false;

					if(getLocalTag() == null && sipResponse.getTo().getTag() != null
							&& SIPTransactionStack.isDialogCreated(cseqMethod) && cseqMethod.equals(getMethod())) {
						setLocalTag(sipResponse.getTo().getTag());
						doPutDialog = true;
					}

					if(statusCode / 100 != 2) {
						if(statusCode / 100 == 1 && doPutDialog) {
							setState(SIPDialog.EARLY_STATE);

							this.setDialogId(sipResponse.getDialogId(true));

							sipStack.putDialog(this);
						} else {
							/*
							 * RFC 3265 chapter 3.1.4.1 "Non-200 class final responses indicate that no
							 * subscription or dialog has been created, and no subsequent NOTIFY message
							 * will be sent. All non-200 class" + responses (with the exception of "489",
							 * described herein) have the same meanings and handling as described in SIP"
							 */

							// https://jain-sip.dev.java.net/servlets/ReadMsg?list=users&msgNo=797
							if(statusCode == 489
									&& (cseqMethod.equals(Request.NOTIFY) || cseqMethod.equals(Request.SUBSCRIBE))) {
								logger.log(Level.FINEST, "RFC 3265 : Not setting dialog to TERMINATED for 489");
							} else {
								/*
								 * simplest fix to https://jain-sip.dev.java.net/issues/show_bug.cgi?id=175 application
								 * is responsible for terminating in this case see rfc 5057 for better explanation
								 */
								if(!this.isReInvite() && getState() != DialogState.CONFIRMED) {
									this.setState(SIPDialog.TERMINATED_STATE);
								}
							}
						}
					} else {
						/*
						 * RFC4235 says that when sending 2xx on UAS side, state should move to CONFIRMED
						 */
						if(this.dialogState <= SIPDialog.EARLY_STATE && (cseqMethod.equals(Request.INVITE)
								|| cseqMethod.equals(Request.SUBSCRIBE) || cseqMethod.equals(Request.REFER))) {
							this.setState(SIPDialog.CONFIRMED_STATE);
						}

						if(doPutDialog) {
							this.setDialogId(sipResponse.getDialogId(true));

							sipStack.putDialog(this);
						}
					}
				}
			}
		} finally {
			if (sipResponse.getCSeq().getMethod().equals(Request.INVITE) && transaction != null
					&& transaction instanceof ClientTransaction && this.getState() != DialogState.TERMINATED) {
				this.acquireTimerTaskSem();

				try {
					if(this.getState() == DialogState.EARLY) {
						if(this.earlyStateTimerTask != null) {
							sipStack.getTimer().cancel(this.earlyStateTimerTask);
						}

						logger.log(Level.FINEST, "EarlyStateTimerTask craeted: {0}", this.earlyDialogTimeout * 1000L);

						this.earlyStateTimerTask = new EarlyStateTimerTask();

						if(sipStack.getTimer() != null && sipStack.getTimer().isStarted()) {
							sipStack.getTimer().schedule(this.earlyStateTimerTask, this.earlyDialogTimeout * 1000L);
						}
					} else {
						if(this.earlyStateTimerTask != null) {
							sipStack.getTimer().cancel(this.earlyStateTimerTask);

							this.earlyStateTimerTask = null;
						}
					}
				} finally {
					this.releaseTimerTaskSem();
				}
			}
		}
	}

	/**
	 * Start the retransmit timer.
	 * 
	 * @param sipServerTx -- server transaction on which the response was sent
	 * @param response    - response that was sent.
	 */
	public void startRetransmitTimer(SIPServerTransaction sipServerTx, Response response) {
		logger.log(Level.FINEST, "startRetransmitTimer() {0} method {1}",
				new Object[] {response.getStatusCode(), sipServerTx.getMethod()});

		if(sipServerTx.isInviteTransaction() && response.getStatusCode() / 100 == 2) {
			this.startTimer(sipServerTx);
		}
	}

	/**
	 * Do target refresh dialog state updates.
	 * 
	 * RFC 3261: Requests within a dialog MAY contain Record-Route and Contact
	 * header fields. However, these requests do not cause the dialog's route set to
	 * be modified, although they may modify the remote target URI. Specifically,
	 * requests that are not target refresh requests do not modify the dialog's
	 * remote target URI, and requests that are target refresh requests do. For
	 * dialogs that have been established with an
	 * 
	 * INVITE, the only target refresh request defined is re-INVITE (see Section
	 * 14). Other extensions may define different target refresh requests for
	 * dialogs established in other ways.
	 */
	private void doTargetRefresh(SIPMessage sipMessage) {
		ContactList contactList = sipMessage.getContactHeaders();

		/*
		 * INVITE is the target refresh for INVITE dialogs. SUBSCRIBE is the target
		 * refresh for subscribe dialogs from the client side. This modifies the remote
		 * target URI potentially
		 */
		if(contactList != null) {
			Contact contact = (Contact) contactList.getFirst();

			this.setRemoteTarget(contact);
		}
	}

	private static final boolean optionPresent(ListIterator l, String option) {
		while(l.hasNext()) {
			OptionTag opt = (OptionTag) l.next();

			if(opt != null && option.equalsIgnoreCase(opt.getOptionTag())) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#createReliableProvisionalResponse(int)
	 */
	@Override
	public Response createReliableProvisionalResponse(int statusCode) throws InvalidArgumentException, SipException {
		if(!(firstTransactionIsServerTransaction)) {
			throw new SipException("Not a Server Dialog!");
		}

		/*
		 * A UAS MUST NOT attempt to send a 100 (Trying) response reliably. Only
		 * provisional responses numbered 101 to 199 may be sent reliably. If the
		 * request did not include either a Supported or Require header field indicating
		 * this feature, the UAS MUST NOT send the provisional response reliably.
		 */
		if(statusCode <= 100 || statusCode > 199) {
			throw new InvalidArgumentException("Bad status code ");
		}

		SIPRequest request = this.originalRequest;
		if(!request.getMethod().equals(Request.INVITE)) {
			throw new SipException("Bad method");
		}

		ListIterator<SIPHeader> list = request.getHeaders(SupportedHeader.NAME);
		if(list == null || !optionPresent(list, "100rel")) {
			list = request.getHeaders(RequireHeader.NAME);

			if(list == null || !optionPresent(list, "100rel")) {
				throw new SipException("No Supported/Require 100rel header in the request");
			}
		}

		SIPResponse response = request.createResponse(statusCode);
		/*
		 * The provisional response to be sent reliably is constructed by the UAS core
		 * according to the procedures of Section 8.2.6 of RFC 3261. In addition, it
		 * MUST contain a Require header field containing the option tag 100rel, and
		 * MUST include an RSeq header field. The value of the header field for the
		 * first reliable provisional response in a transaction MUST be between 1 and
		 * 231 - 1. It is RECOMMENDED that it be chosen uniformly in this range. The
		 * RSeq numbering space is within a single transaction. This means that
		 * provisional responses for different requests MAY use the same values for the
		 * RSeq number.
		 */
		Require require = new Require();
		try {
			require.setOptionTag("100rel");
		} catch(Exception ex) {
			InternalErrorHandler.handleException(ex);
		}

		response.addHeader(require);

		RSeq rseq = new RSeq();

		/*
		 * set an arbitrary sequence number. This is actually set when the response is
		 * sent out
		 */
		rseq.setSeqNumber(1L);

		/*
		 * Copy the record route headers from the request to the response ( Issue 160 ).
		 * Note that other 1xx headers do not get their Record Route headers copied over
		 * but reliable provisional responses do. See RFC 3262 Table 2.
		 */
		RecordRouteList rrl = request.getRecordRouteHeaders();

		if(rrl != null) {
			RecordRouteList rrlclone = (RecordRouteList) rrl.clone();

			response.setHeader(rrlclone);
		}

		return response;
	}

	/**
	 * Do the processing necessary for the PRACK
	 * 
	 * @param prackRequest
	 * @return true if this is the first time the TX has seen the prack ( and hence
	 *         needs to be passed up to the TU)
	 */
	public boolean handlePrack(SIPRequest prackRequest) {
		/*
		 * The RAck header is sent in a PRACK request to support reliability of
		 * provisional responses. It contains two numbers and a method tag. The first
		 * number is the value from the RSeq header in the provisional response that is
		 * being acknowledged. The next number, and the method, are copied from the CSeq
		 * in the response that is being acknowledged. The method name in the RAck
		 * header is case sensitive.
		 */
		if(!this.isServer()) {
			logger.log(Level.FINEST, "Dropping Prack -- not a server Dialog");

			return false;
		}

		SIPServerTransaction sipServerTransaction = (SIPServerTransaction) this.getFirstTransactionInt();
		byte[] sipResponse = sipServerTransaction.getReliableProvisionalResponse();

		if(sipResponse == null) {
			logger.log(Level.FINEST, "Dropping Prack -- ReliableResponse not found");

			return false;
		}

		RAck rack = (RAck) prackRequest.getHeader(RAckHeader.NAME);
		if(rack == null) {
			logger.log(Level.FINEST, "Dropping Prack -- rack header not found");

			return false;
		}

		if(!rack.getMethod().equals(sipServerTransaction.getPendingReliableResponseMethod())) {
			logger.log(Level.FINEST, "Dropping Prack -- CSeq Header does not match PRACK");

			return false;
		}

		if(rack.getCSeqNumberLong() != sipServerTransaction.getPendingReliableCSeqNumber()) {
			logger.log(Level.FINEST, "Dropping Prack -- CSeq Header does not match PRACK");

			return false;
		}

		if(rack.getRSequenceNumber() != sipServerTransaction.getPendingReliableRSeqNumber()) {
			logger.log(Level.FINEST, "Dropping Prack -- RSeq Header does not match PRACK");

			return false;
		}

		return sipServerTransaction.prackRecieved();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.sip.Dialog#sendReliableProvisionalResponse(javax.sip.message.Response )
	 */
	@Override
	public void sendReliableProvisionalResponse(Response relResponse) throws SipException {
		if(!this.isServer()) {
			throw new SipException("Not a Server Dialog");
		}

		SIPResponse sipResponse = (SIPResponse) relResponse;
		if(relResponse.getStatusCode() == 100) {
			throw new SipException("Cannot send 100 as a reliable provisional response");
		}

		if(relResponse.getStatusCode() / 100 > 2) {
			throw new SipException("Response code is not a 1xx response - should be in the range 101 to 199 ");
		}

		/*
		 * Do a little checking on the outgoing response.
		 */
		if(sipResponse.getToTag() == null) {
			throw new SipException("Badly formatted response -- To tag mandatory for Reliable Provisional Response");
		}

		ListIterator requireList = (ListIterator) relResponse.getHeaders(RequireHeader.NAME);
		boolean found = false;

		if(requireList != null) {
			while(requireList.hasNext() && !found) {
				RequireHeader rh = (RequireHeader) requireList.next();

				if(rh.getOptionTag().equalsIgnoreCase("100rel")) {
					found = true;
				}
			}
		}

		if(!found) {
			Require require = new Require("100rel");
			relResponse.addHeader(require);

			logger.log(Level.FINEST, "Require header with optionTag 100rel is needed -- adding one");
		}

		SIPServerTransaction serverTransaction = (SIPServerTransaction) this.getFirstTransactionInt();

		/*
		 * put into the dialog table before sending the response so as to avoid race
		 * condition with PRACK
		 */
		this.setLastResponse(serverTransaction, sipResponse);

		this.setDialogId(sipResponse.getDialogId(true));

		serverTransaction.sendReliableProvisionalResponse(relResponse);

		this.startRetransmitTimer(serverTransaction, relResponse);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.Dialog#terminateOnBye(boolean)
	 */
	@Override
	public void terminateOnBye(boolean terminateFlag) throws SipException {
		this.terminateOnBye = terminateFlag;
	}

	/**
	 * Set the "assigned" flag to true. We do this when inserting the dialog into
	 * the dialog table of the stack.
	 * 
	 */
	public void setAssigned() {
		this.isAssigned = true;
	}

	/**
	 * Return true if the dialog has already been mapped to a transaction.
	 * 
	 */
	public boolean isAssigned() {
		return this.isAssigned;
	}

	/**
	 * Get the contact header that the owner of this dialog assigned. Subsequent
	 * Requests are considered to belong to the dialog if the dialog identifier
	 * matches and the contact header matches the ip address and port on which the
	 * request is received.
	 * 
	 * @return contact header belonging to the dialog.
	 */
	public Contact getMyContactHeader() {
		if(contactHeader == null && contactHeaderStringified != null) {
			try {
				this.contactHeader = (Contact) new ContactParser(contactHeaderStringified).parse();
			} catch(ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the contact header", e);
			}
		}

		return contactHeader;
	}

	/**
	 * Do the necessary processing to handle an ACK directed at this Dialog.
	 * 
	 * @param ackTransaction -- the ACK transaction that was directed at this
	 *                       dialog.
	 * @return -- true if the ACK was successfully consumed by the Dialog and
	 *         resulted in the dialog state being changed.
	 */
	public boolean handleAck(SIPServerTransaction ackTransaction) {
		if(isAckSeen() && getRemoteSeqNumber() == ackTransaction.getCSeq()) {
			logger.log(Level.FINEST, "SIPDialog::handleAck: ACK already seen by dialog -- dropping Ack retransmission");

			acquireTimerTaskSem();
			try {
				if(this.timerTask != null) {
					this.getStack().getTimer().cancel(timerTask);

					this.timerTask = null;
				}
			} finally {
				releaseTimerTaskSem();
			}

			return false;
		}

		if(this.getState() == DialogState.TERMINATED) {
			logger.log(Level.FINEST, "SIPDialog::handleAck: Dialog is terminated -- dropping ACK");

			return false;
		}

		logger.log(Level.FINEST, "SIPDialog::handleAck: lastResponseCSeqNumber: {0} ackTxCSeq {1}",
				new Object[] { lastInviteOkReceived, ackTransaction.getCSeq() });

		if(lastResponseStatusCode != null && this.lastInviteResponseCode / 100 == 2
				&& lastInviteResponseCSeqNumber == ackTransaction.getCSeq()) {
			ackTransaction.setDialog(this, lastResponseDialogId);

			/*
			 * record that we already saw an ACK for this dialog.
			 */
			ackReceived(ackTransaction.getCSeq());

			logger.log(Level.FINEST, "SIPDialog::handleACK: ACK for 2XX response --- sending to TU ");

			return true;
		}

		/*
		 * This happens when the ACK is re-transmitted and arrives too late to be
		 * processed.
		 */
		logger.log(Level.FINEST, "INVITE transaction not found");

		if(this.isBackToBackUserAgent()) {
			this.releaseAckSem();
		}

		return false;
	}

	String getEarlyDialogId() {
		return earlyDialogId;
	}

	/**
	 * Release the semaphore for ACK processing so the next re-INVITE may proceed.
	 */
	void releaseAckSem() {
		logger.log(Level.FINEST, "releaseAckSem-enter]] {0}, sem: {1}, b2bua: {2}",
				new Object[] { this, this.ackSem, this.isBackToBackUserAgent });

		if(this.isBackToBackUserAgent) {
			logger.log(Level.FINEST, "releaseAckSem]] {0}, sem: {1}", new Object[] { this, this.ackSem });

			if(this.ackSem.availablePermits() == 0) {
				this.ackSem.release();

				logger.log(Level.FINEST, "releaseAckSem]] {0} sem: {1}", new Object[] {this, this.ackSem});
			}
		}
	}

	boolean isBlockedForReInvite() {
		return this.ackSem.availablePermits() == 0;
	}

	boolean takeAckSem() {
		logger.log(Level.FINEST, "[takeAckSem {0}, sem: {1}", new Object[] { this, this.ackSem });

		try {
			if(!this.ackSem.tryAcquire(2, TimeUnit.SECONDS)) {
				logger.log(Level.SEVERE, "Cannot aquire ACK semaphore ");

				logger.log(Level.FINEST, "Semaphore previously acquired at {0} sem: {1}",
						new Object[] {this.stackTrace, this.ackSem});

				return false;
			}
		} catch(InterruptedException ex) {
			logger.log(Level.SEVERE, "Cannot aquire ACK semaphore");

			return false;
		}

		return true;
	}

	/**
	 * @param lastAckSent the lastAckSent to set
	 */
	private void setLastAckSent(SIPRequest lastAckSent) {
		this.lastAckSent = lastAckSent;
		this.lastAckSent.setTransaction(null); // null out the associated TX (release memory)
	}

	/**
	 * @return true if an ACK was ever sent for this Dialog
	 */
	public boolean isAtleastOneAckSent() {
		return this.isAcknowledged;
	}

	public boolean isBackToBackUserAgent() {
		return this.isBackToBackUserAgent;
	}

	public synchronized void doDeferredDeleteIfNoAckSent(long seqno) {
		if(sipStack.getTimer() == null) {
			this.setState(TERMINATED_STATE);
		} else if(dialogDeleteIfNoAckSentTask == null) {
			// Delete the transaction after the max ACK timeout.
			dialogDeleteIfNoAckSentTask = new DialogDeleteIfNoAckSentTask(seqno);

			if(sipStack.getTimer() != null && sipStack.getTimer().isStarted()) {
				int delay = SIPTransactionStack.BASE_TIMER_INTERVAL;

				if(lastTransaction != null) {
					delay = lastTransaction.getBaseTimerInterval();
				}

				sipStack.getTimer().schedule(dialogDeleteIfNoAckSentTask, sipStack.getAckTimeoutFactor() * delay);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.DialogExt#setBackToBackUserAgent(boolean)
	 */
	@Override
	public void setBackToBackUserAgent() {
		this.isBackToBackUserAgent = true;
	}

	/**
	 * @return the eventHeader
	 */
	EventHeader getEventHeader() {
		return eventHeader;
	}

	/**
	 * @param eventHeader the eventHeader to set
	 */
	void setEventHeader(EventHeader eventHeader) {
		this.eventHeader = eventHeader;
	}

	/**
	 * @param serverTransactionFlag the serverTransactionFlag to set
	 */
	void setServerTransactionFlag(boolean serverTransactionFlag) {
		this.serverTransactionFlag = serverTransactionFlag;
	}

	/**
	 * @param reInviteFlag the reinviteFlag to set
	 */
	protected void setReInviteFlag(boolean reInviteFlag) {
		this.reInviteFlag = reInviteFlag;
	}

	public boolean isSequenceNumberValidation() {
		return this.sequenceNumberValidation;
	}

	@Override
	public void disableSequenceNumberValidation() {
		this.sequenceNumberValidation = false;
	}

	public void acquireTimerTaskSem() {
		boolean acquired = false;
		try {
			acquired = this.timerTaskLock.tryAcquire(10, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			acquired = false;
		}

		if(!acquired) {
			throw new IllegalStateException("Impossible to acquire the dialog timer task lock");
		}
	}

	public void releaseTimerTaskSem() {
		this.timerTaskLock.release();
	}

	public String getMergeId() {
		return this.firstTransactionMergeId;
	}

	public void setPendingRouteUpdateOn202Response(SIPRequest sipRequest) {
		this.pendingRouteUpdateOn202Response = true;

		// Issue 374 : get the from tag instead of to tag
		String fromTag = sipRequest.getFromHeader().getTag();
		if(fromTag != null) {
			this.setRemoteTag(fromTag);
		}
	}

	public String getLastResponseMethod() {
		return lastResponseMethod;
	}

	public Integer getLastResponseStatusCode() {
		return lastResponseStatusCode;
	}

	public long getLastResponseCSeqNumber() {
		return lastResponseCSeqNumber;
	}

	// cleanup the dialog from the data not needed anymore upon receiving
	// or sending an ACK
	// to save on mem
	protected void cleanUpOnAck() {
		if(isReleaseReferences()) {
			logger.log(Level.FINEST, "cleanupOnAck: {0}", getDialogId());

			if(originalRequest != null) {
				if (originalRequestRecordRouteHeaders != null) {
					originalRequestRecordRouteHeadersString = originalRequestRecordRouteHeaders.toString();
				}

				originalRequestRecordRouteHeaders = null;
				originalRequest = null;
			}

			if(firstTransaction != null) {
				if(firstTransaction.getOriginalRequest() != null) {
					firstTransaction.getOriginalRequest().cleanUp();
				}

				firstTransaction = null;
			}

			if(lastTransaction != null) {
				if(lastTransaction.getOriginalRequest() != null) {
					lastTransaction.getOriginalRequest().cleanUp();
				}

				lastTransaction = null;
			}

			if(callIdHeader != null) {
				callIdHeaderString = callIdHeader.toString();
				callIdHeader = null;
			}

			if(contactHeader != null) {
				contactHeaderStringified = contactHeader.toString();
				contactHeader = null;
			}

			if(remoteTarget != null) {
				remoteTargetStringified = remoteTarget.toString();
				remoteTarget = null;
			}

			if(remoteParty != null) {
				remotePartyStringified = remoteParty.toString();
				remoteParty = null;
			}

			if(localParty != null) {
				localPartyStringified = localParty.toString();
				localParty = null;
			}
		}
	}

	/**
	 * Release references so the GC can clean up dialog state.
	 * 
	 */
	protected void cleanUp() {
		if(isReleaseReferences()) {
			cleanUpOnAck();

			logger.log(Level.FINEST, "dialog cleanup: {0}", getDialogId());

			if(eventListeners != null) {
				eventListeners.clear();
			}

			timerTaskLock = null;
			ackSem = null;
			contactHeader = null;
			eventHeader = null;
			firstTransactionId = null;
			firstTransactionMethod = null;

			// Cannot clear up the last ACK Sent. until DIALOG is terminated.

			lastResponseDialogId = null;
			lastResponseMethod = null;
			lastResponseTopMostVia = null;

			if(originalRequestRecordRouteHeaders != null) {
				originalRequestRecordRouteHeaders.clear();
				originalRequestRecordRouteHeaders = null;
				originalRequestRecordRouteHeadersString = null;
			}

			if(routeList != null) {
				routeList.clear();
				routeList = null;
			}

			responsesReceivedInForkingCase.clear();
		}
	}

	protected RecordRouteList getOriginalRequestRecordRouteHeaders() {
		if(originalRequestRecordRouteHeaders == null && originalRequestRecordRouteHeadersString != null) {
			try {
				originalRequestRecordRouteHeaders = (RecordRouteList) new RecordRouteParser(
						originalRequestRecordRouteHeadersString).parse();
			} catch(ParseException e) {
				logger.log(Level.SEVERE, "error reparsing the originalRequest RecordRoute Headers", e);
			}

			originalRequestRecordRouteHeadersString = null;
		}

		return originalRequestRecordRouteHeaders;
	}

	/**
	 * @return the lastResponseTopMostVia
	 */
	public Via getLastResponseTopMostVia() {
		return lastResponseTopMostVia;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.DialogExt#isReleaseReferences()
	 */
	@Override
	public boolean isReleaseReferences() {
		return releaseReferences;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.DialogExt#setReleaseReferences(boolean)
	 */
	@Override
	public void setReleaseReferences(boolean releaseReferences) {
		this.releaseReferences = releaseReferences;
	}

	@Override
	public void setEarlyDialogTimeoutSeconds(int seconds) {
		if(seconds <= 0) {
			throw new IllegalArgumentException("Invalid value " + seconds);
		}

		this.earlyDialogTimeout = seconds;
	}

	public void checkRetransmissionForForking(SIPResponse response) {
		final int statusCode = response.getStatusCode();
		final String responseMethod = response.getCSeqHeader().getMethod();
		final long responseCSeqNumber = response.getCSeq().getSeqNumber();
		boolean isRetransmission = !responsesReceivedInForkingCase
				.add(statusCode + "/" + responseCSeqNumber + "/" + responseMethod);

		response.setRetransmission(isRetransmission);

		logger.log(Level.FINEST, "marking response as retransmission {0}, for dialog: {1}",
				new Object[] {isRetransmission, this});
	}

	@Override
	public int hashCode() {
		// https://java.net/jira/browse/JSIP-493
		if((callIdHeader == null) && (callIdHeaderString == null)) {
			return 0;
		}

		return getCallId().getCallId().hashCode();
	}

	/**
	 * In case of forking scenarios, set the original dialog that had been forked
	 * 
	 * @param defaultDialog
	 */
	public void setOriginalDialog(SIPDialog originalDialog) {
		this.originalDialog = originalDialog;
	}

	@Override
	public boolean isForked() {
		return originalDialog != null;
	}

	@Override
	public Dialog getOriginalDialog() {
		return originalDialog;
	}

	/**
	 * Set the ACK sending strategy to be used by this dialog
	 * 
	 * @param ackSendingStrategy
	 */
	public void setAckSendingStrategy(AckSendingStrategy ackSendingStrategy) {
		this.ackSendingStrategy = ackSendingStrategy;
	}
}
