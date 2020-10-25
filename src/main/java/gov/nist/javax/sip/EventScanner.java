package gov.nist.javax.sip;

import java.util.EventObject;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.core.ThreadAuditor;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;

/**
 * Event Scanner to deliver events to the Listener.
 */
public class EventScanner implements Runnable {
	private static final Logger logger = Logger.getLogger(EventScanner.class.getName());

	private boolean isStopped;
	private int refCount;

	// SIPquest: Fix for deadlocks
	private LinkedList<EventWrapper> pendingEvents;

	private int[] eventMutex = { 0 };

	private SipStackImpl sipStack;

	public void incrementRefcount() {
		synchronized (eventMutex) {
			this.refCount++;
		}
	}

	public EventScanner(SipStackImpl sipStackImpl) {
		this.pendingEvents = new LinkedList<>();

		Thread myThread = new Thread(this);

		// This needs to be set to false else the main thread mysteriously exits.
		myThread.setDaemon(false);

		this.sipStack = sipStackImpl;

		myThread.setName("EventScannerThread");

		myThread.start();
	}

	public void addEvent(EventWrapper eventWrapper) {
		logger.log(Level.FINEST, "addEvent {0}", eventWrapper);

		synchronized(this.eventMutex) {
			pendingEvents.add(eventWrapper);

			// Add the event into the pending events list
			eventMutex.notify();
		}
	}

	/**
	 * Stop the event scanner. Decrement the reference count and exit the scanner thread if the reference count
	 * goes to 0.
	 */
	public void stop() {
		synchronized (eventMutex) {
			if(this.refCount > 0) {
				this.refCount--;
			}

			if(this.refCount == 0) {
				isStopped = true;

				eventMutex.notify();
			}
		}
	}

	/**
	 * Brutally stop the event scanner. This does not wait for the reference count to go to 0.
	 */
	public void forceStop() {
		synchronized(this.eventMutex) {
			this.isStopped = true;
			this.refCount = 0;

			this.eventMutex.notify();
		}
	}

	public void deliverEvent(EventWrapper eventWrapper) {
		EventObject sipEvent = eventWrapper.sipEvent;

		logger.log(Level.FINEST, "sipEvent: {0}, source: {1}", new Object[] {sipEvent, sipEvent.getSource()});

		SipListener sipListener = null;
		if(!(sipEvent instanceof IOExceptionEvent)) {
			sipListener = ((SipProviderImpl) sipEvent.getSource()).getSipListener();
		} else {
			sipListener = sipStack.getSipListener();
		}

		if(sipEvent instanceof RequestEvent) {
			try {
				// Check if this request has already created a transaction
				SIPRequest sipRequest = (SIPRequest) ((RequestEvent) sipEvent).getRequest();

				logger.log(Level.FINEST, "deliverEvent: {0}, transaction: {1}, sipEvent.serverTx: {2}",
						new Object[] {sipRequest.getFirstLine(), eventWrapper.transaction,
								((RequestEvent) sipEvent).getServerTransaction()});

				/*
				 * Discard the duplicate request if a transaction already exists. If the listener chose to handle
				 * the request in a stateless way, then the listener will see the retransmission. Note that in both of
				 * these two cases, JAIN SIP will allow you to handle the request in a stateful or stateless way.
				 * An example of the latter case is REGISTER and an example of the former case is INVITE.
				 */
				SIPServerTransaction tx = (SIPServerTransaction) sipStack.findTransaction(sipRequest, true);

				if(tx != null && !tx.passToListener()) {
					/*
					 * make an exception for a very rare case: some (broken) UACs use the same branch parameter for
					 * an ACK. Such an ACK should be passed to the listener (TX == INVITE ST, terminated upon sending
					 * 2xx but lingering to catch retransmitted INVITEs)
					 */
					if(sipRequest.getMethod().equals(Request.ACK) && tx.isInviteTransaction()
							&& (tx.getLastResponseStatusCode() / 100 == 2 || sipStack.isNon2XXAckPassedToListener())) {
						logger.log(Level.FINEST, "Detected broken client sending ACK with same branch! Passing...");
					} else {
						logger.log(Level.FINEST, "transaction already exists! {0}", tx);

						return;
					}
				} else if(sipStack.findPendingTransaction(sipRequest.getTransactionId()) != null) {
					logger.log(Level.FINEST, "transaction already exists!!");

					return;
				} else {
					/*
					 * Put it in the pending list so that if a repeat request comes along it will not get assigned
					 * a new transaction
					 */
					SIPServerTransaction st = (SIPServerTransaction) eventWrapper.transaction;

					sipStack.putPendingTransaction(st);
				}

				// Set up a pointer to the transaction.
				sipRequest.setTransaction(eventWrapper.transaction);

				try {
					logger.log(Level.FINEST, "Calling listener: {0}", sipRequest.getFirstLine());
					logger.log(Level.FINEST, "Calling listener: {0}", eventWrapper.transaction);

					if(sipListener != null) {
						sipListener.processRequest((RequestEvent) sipEvent);
					}

					logger.log(Level.FINEST, "Done processing Message: {0}", sipRequest.getFirstLine());

					if(null != eventWrapper.transaction) {
						SIPDialog dialog = (SIPDialog) eventWrapper.transaction.getDialog();

						if(dialog != null) {
							dialog.requestConsumed();
						}
					}
				} catch(Exception ex) {
					/*
					 * We cannot let this thread die under any circumstances. Protect ourselves by logging errors
					 * to the console but continue.
					 */
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			} finally {
				logger.log(Level.FINEST, "Done processing Message: {0}",
						((SIPRequest) (((RequestEvent) sipEvent).getRequest())).getFirstLine());

				if(eventWrapper.transaction != null
						&& ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
					((SIPServerTransaction) eventWrapper.transaction).releaseSem();
				}

				if(eventWrapper.transaction != null) {
					sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
				}

				if(eventWrapper.transaction.getMethod().equals(Request.ACK)) {
					/*
					 * Set the tx state to terminated so it is removed from the stack if the user configured
					 * to get notification on ACK termination
					 */

					eventWrapper.transaction.setState(TransactionState.TERMINATED_VALUE);
				}
			}
		} else if(sipEvent instanceof ResponseEvent) {
			try {
				ResponseEvent responseEvent = (ResponseEvent) sipEvent;
				SIPResponse sipResponse = (SIPResponse) responseEvent.getResponse();
				SIPDialog sipDialog = ((SIPDialog) responseEvent.getDialog());

				try {
					logger.log(Level.FINEST, "Calling listener {0} for {1}",
							new Object[] {sipListener, sipResponse.getFirstLine()});

					if(sipListener != null) {
						SIPTransaction tx = eventWrapper.transaction;

						if(tx != null) {
							tx.setPassToListener();
						}

						sipListener.processResponse((ResponseEvent) sipEvent);
					}

					/*
					 * If the response for a request within a dialog is a 481 (Call/Transaction Does Not Exist)
					 * or a 408 (Request Timeout), the UAC SHOULD terminate the dialog.
					 */
					if((sipDialog != null
							&& (sipDialog.getState() == null || !sipDialog.getState().equals(DialogState.TERMINATED)))
							&& (sipResponse.getStatusCode() == Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST
									|| sipResponse.getStatusCode() == Response.REQUEST_TIMEOUT)) {
						logger.log(Level.FINEST, "Removing dialog on 408 or 481 response");

						sipDialog.doDeferredDelete();
					}

					/*
					 * The Client TX disappears after the first 2xx response However, additional 2xx
					 * responses may arrive later for example in the following scenario:
					 *
					 * Multiple 2xx responses may arrive at the UAC for a single INVITE request due
					 * to a forking proxy. Each response is distinguished by the tag parameter in
					 * the To header field, and each represents a distinct dialog, with a distinct
					 * dialog identifier.
					 *
					 * If the Listener does not ACK the 200 then we assume he does not care about
					 * the dialog and GC the dialog after some time. However, this is really an
					 * application bug. This garbage collects unacknowledged dialogs.
					 *
					 */
					if(sipResponse.getCSeq().getMethod().equals(Request.INVITE)
							&& sipDialog != null && sipResponse.getStatusCode() == 200) {
						logger.log(Level.FINEST, "Warning! unacknowledged dialog: {0}", sipDialog.getState());

						/*
						 * If we don't see an ACK in 32 seconds, we want to tear down the dialog.
						 */
						sipDialog.doDeferredDeleteIfNoAckSent(sipResponse.getCSeq().getSeqNumber());
					}
				} catch(Exception ex) {
					/*
					 * We cannot let this thread die under any circumstances. Protect ourselves by logging errors
					 * to the console but continue.
					 */
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}

				/*
				 * The original request is not needed except for INVITE transactions -- null the pointers
				 * to the transactions so that state may be released.
				 */
				SIPClientTransaction ct = (SIPClientTransaction) eventWrapper.transaction;
				if(ct != null && TransactionState.COMPLETED_VALUE == ct.getInternalState()
						&& !ct.getMethod().equals(Request.INVITE)) {
					/*
					 * reduce the state to minimum this assumes that the application will not need to access
					 * the request once the transaction is completed.
					 */
					ct.clearState();
				}

				// mark no longer in the event queue.
			} finally {
				if(eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
					eventWrapper.transaction.releaseSem();
				}
			}
		} else if(sipEvent instanceof TimeoutEvent) {
			try {
				// Check for null as listener could be removed.
				if(sipListener != null) {
					sipListener.processTimeout((TimeoutEvent) sipEvent);
				}
			} catch(Exception ex) {
				/*
				 * We cannot let this thread die under any circumstances. Protect ourselves by logging errors
				 * to the console but continue.
				 */
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} else if(sipEvent instanceof DialogTimeoutEvent) {
			try {
				// Check for null as listener could be removed.
				if(sipListener instanceof SipListenerExt) {
					((SipListenerExt) sipListener).processDialogTimeout((DialogTimeoutEvent) sipEvent);
				} else {
					logger.log(Level.FINEST, "DialogTimeoutEvent not delivered");
				}
			} catch(Exception ex) {
				/*
				 * We cannot let this thread die under any circumstances. Protect ourselves by logging errors
				 * to the console but continue.
				 */

				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} else if (sipEvent instanceof IOExceptionEvent) {
			try {
				if(sipListener != null) {
					sipListener.processIOException((IOExceptionEvent) sipEvent);
				}
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} else if(sipEvent instanceof TransactionTerminatedEvent) {
			try {
				logger.log(Level.FINEST, "About to deliver transactionTerminatedEvent");
				logger.log(Level.FINEST, "tx: {0}", ((TransactionTerminatedEvent) sipEvent).getClientTransaction());
				logger.log(Level.FINEST, "tx: {0}", ((TransactionTerminatedEvent) sipEvent).getServerTransaction());

				if(sipListener != null) {
					sipListener.processTransactionTerminated((TransactionTerminatedEvent) sipEvent);
				}
			} catch(AbstractMethodError ame) {
				// for backwards compatibility, accept this
				logger.log(Level.WARNING, "Unable to call sipListener.processTransactionTerminated");
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} else if(sipEvent instanceof DialogTerminatedEvent) {
			try {
				if(sipListener != null) {
					sipListener.processDialogTerminated((DialogTerminatedEvent) sipEvent);
				}
			} catch(AbstractMethodError ame) {
				// for backwards compatibility, accept this
				logger.log(Level.WARNING, "Unable to call sipListener.processDialogTerminated");
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} else {
			logger.log(Level.SEVERE, "bad event: {0}", sipEvent);
		}
	}

	/**
	 * For the non-reentrant listener this delivers the events to the listener from a single queue. If the listener
	 * is reentrant, then the stack just calls the deliverEvent method above.
	 */
	@Override
	public void run() {
		try {
			// Ask the auditor to monitor this thread
			ThreadAuditor.ThreadHandle threadHandle = sipStack.getThreadAuditor().addCurrentThread();

			while(true) {
				EventWrapper eventWrapper = null;

				LinkedList<EventWrapper> eventsToDeliver;
				synchronized(this.eventMutex) {
					// First, wait for some events to become available.
					while (pendingEvents.isEmpty()) {
						/*
						 * There's nothing in the list, check to make sure we haven't been stopped. If we have,
						 * then let the thread die.
						 */
						if(this.isStopped) {
							logger.log(Level.FINEST, "Stopped event scanner!!");

							return;
						}

						/*
						 * We haven't been stopped, and the event list is indeed rather empty. Wait for some events
						 * to come along.
						 */
						try {
							// Send a heart beat to the thread auditor
							threadHandle.ping();

							// Wait for events (with a timeout)
							eventMutex.wait(threadHandle.getPingIntervalInMillisecs());
						} catch(InterruptedException ex) {
							// Let the thread die a normal death
							logger.log(Level.FINEST, "Interrupted!");

							return;
						}
					}

					/*
					 * There are events in the 'pending events list' that need processing. Hold onto the old
					 * 'pending Events' list, but make a new one for the other methods to operate on. This tap-dancing
					 * is to avoid deadlocks and also to ensure that the list is not modified while we are
					 * iterating over it.
					 */
					eventsToDeliver = pendingEvents;

					pendingEvents = new LinkedList<>();
				}

				ListIterator<EventWrapper> iterator = eventsToDeliver.listIterator();
				while(iterator.hasNext()) {
					eventWrapper = iterator.next();

					logger.log(Level.FINEST, "Processing, eventWrapper: {0} eventsToDeliver.size(): {1}",
							new Object[] {eventWrapper, eventsToDeliver.size()});

					try {
						deliverEvent(eventWrapper);
					} catch(Exception e) {
						logger.log(Level.SEVERE, "Unexpected exception caught while delivering event --"
								+ " carrying on bravely", e);
					}
				}
			}
		} finally {
			if(!this.isStopped) {
				logger.log(Level.SEVERE, "Event scanner exited abnormally");
			}
		}
	}
}
