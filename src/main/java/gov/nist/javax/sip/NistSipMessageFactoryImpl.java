package gov.nist.javax.sip;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.TransactionState;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import gov.nist.javax.sip.stack.StackMessageFactory;

/**
 * Implements all the support classes that are necessary for the nist-sip stack
 * on which the jain-sip stack has been based. This is a mapping class to map
 * from the NIST-SIP abstractions to the JAIN abstractions. (i.e. It is the glue
 * code that ties the NIST-SIP event model and the JAIN-SIP event model
 * together. When a SIP Request or SIP Response is read from the corresponding
 * messageChannel, the NIST-SIP stack calls the SIPStackMessageFactory
 * implementation that has been registered with it to process the request.)
 */
class NistSipMessageFactoryImpl implements StackMessageFactory {
	private static final Logger logger = Logger.getLogger(NistSipMessageFactoryImpl.class.getName());

	private SIPTransactionStack sipStack;

	/**
	 * Construct a new SIP Server Request.
	 * 
	 * @param sipRequest     is the SIPRequest from which the SIPServerRequest is to
	 *                       be constructed.
	 * @param messageChannel is the MessageChannel abstraction for this
	 *                       SIPServerRequest.
	 */
	public ServerRequestInterface newSIPServerRequest(SIPRequest sipRequest, SIPTransaction sipTransaction) {
		if(null == sipTransaction || null == sipRequest) {
			throw new IllegalArgumentException("Null Arg!");
		}

		SIPTransactionStack theStack = sipTransaction.getSIPStack();
		DialogFilter retval = new DialogFilter(theStack);

		// If the transaction has already been created
		// then set the transaction channel.
		retval.transactionChannel = sipTransaction;
		retval.listeningPoint = sipTransaction.getMessageProcessor().getListeningPoint();

		if(null == retval.listeningPoint) {
			return null;
		}

		logger.log(Level.FINEST, "Returning request interface for {0} {1}, sipTransaction: {2}", new Object[] {
				sipRequest.getFirstLine(), retval, sipTransaction});

		return retval;
	}

	/**
	 * Generate a new server response for the stack.
	 * 
	 * @param sipResponse    is the SIPRequest from which the SIPServerRequest is to
	 *                       be constructed.
	 * @param messageChannel is the MessageChannel abstraction for this
	 *                       SIPServerResponse
	 */
	public ServerResponseInterface newSIPServerResponse(SIPResponse sipResponse, MessageChannel msgChannel) {
		// tr is null if a transaction is not mapped.
		SIPTransaction tr = sipStack.findTransaction(sipResponse, false);

		logger.log(Level.FINEST, "Found Transaction {0} for {1}", new Object[] {tr, sipResponse});

		if(tr != null) {
			/*
			 * Prune unhealthy responses early if handling stateful. If the state has not yet been assigned
			 * then this is a spurious response. This was moved up from the transaction layer for efficiency.
			 */
			if(tr.getInternalState() < 0) {
				logger.log(Level.FINEST, "Dropping response - null transaction state");

				return null;
			} else if(TransactionState.COMPLETED_VALUE == tr.getInternalState() && sipResponse.getStatusCode() / 100 == 1) {
				// Ignore 1xx
				logger.log(Level.FINEST, "Dropping response - late arriving: {0}", sipResponse.getStatusCode());

				return null;
			}
		}

		DialogFilter retval = new DialogFilter(sipStack);

		retval.transactionChannel = tr;

		retval.listeningPoint = msgChannel.getMessageProcessor().getListeningPoint();
		return retval;
	}

	public NistSipMessageFactoryImpl(SIPTransactionStack sipStackImpl) {
		this.sipStack = sipStackImpl;
	}
}
