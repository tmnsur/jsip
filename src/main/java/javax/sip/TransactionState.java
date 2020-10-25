package javax.sip;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class contains the enumerations that define the underlying state of an
 * existing transaction. SIP defines four types of transactions, these are
 * Invite Client transactions, Invite Server transactions, Non-Invite Client
 * transactions and Non-Invite Server transactions.
 *
 * There are six explicit states for the various transactions, namely:
 * <ul>
 * <li><b>Calling:</b>
 * <ul>
 * <li>Invite Client transaction: The initial state, "calling", MUST be entered
 * when the application initiates a new client transaction with an INVITE
 * request.
 * </ul>
 * <li><b>Trying:</b>
 * <ul>
 * <li>Non-Invite Client transaction: The initial state "Trying" is entered when
 * the application initiates a new client transaction with a request.
 * <li>Non-Invite Server transaction: The initial state "Trying" is entered when
 * the application is passed a request other than INVITE or ACK.
 * </ul>
 * <li><b>Proceeding:</b>
 * <ul>
 * <li>Invite Client transaction: If the client transaction receives a
 * provisional response while in the "Calling" state, it transitions to the
 * "Proceeding" state.
 * <li>Non-Invite Client transaction: If a provisional response is received
 * while in the "Trying" state, the client transaction SHOULD move to the
 * "Proceeding" state.
 * <li>Invite Server transaction: When a server transaction is constructed for a
 * request, it enters the initial state "Proceeding".
 * <li>Non-Invite Server transaction: While in the "Trying" state, if the
 * application passes a provisional response to the server transaction, the
 * server transaction MUST enter the "Proceeding" state.
 * </ul>
 * <li><b>Completed:</b> The "Completed" state exists to buffer any additional
 * response retransmissions that may be received, which is why the client
 * transaction remains there only for unreliable transports.
 * <ul>
 * <li>Invite Client transaction: When in either the "Calling" or "Proceeding"
 * states, reception of a response with status code from 300-699 MUST cause the
 * client transaction to transition to "Completed".
 * <li>Non-Invite Client transaction: If a final response (status codes 200-699)
 * is received while in the "Trying" or "Proceeding" state, the client
 * transaction MUST transition to the "Completed" state.
 * <li>Invite Server transaction: While in the "Proceeding" state, if the
 * application passes a response with status code from 300 to 699 to the server
 * transaction, the state machine MUST enter the "Completed" state.
 * <li>Non-Invite Server transaction: If the application passes a final response
 * (status codes 200-699) to the server while in the "Proceeding" state, the
 * transaction MUST enter the "Completed" state.
 * </ul>
 * <li><b>Confirmed:</b> The purpose of the "Confirmed" state is to absorb any
 * additional ACK messages that arrive, triggered from retransmissions of the
 * final response. Once this time expires the server MUST transition to the
 * "Terminated" state.
 * <ul>
 * <li>Invite Server transaction: If an ACK is received while the server
 * transaction is in the "Completed" state, the server transaction MUST
 * transition to the "Confirmed" state.
 * </ul>
 * <li><b>Terminated:</b> The transaction MUST be available for garbage
 * collection the instant it enters the "Terminated" state.
 * <ul>
 * <li>Invite Client transaction: When in either the "Calling" or "Proceeding"
 * states, reception of a 2xx response MUST cause the client transaction to
 * enter the "Terminated" state. If amount of time that the server transaction
 * can remain in the "Completed" state when unreliable transports are used
 * expires while the client transaction is in the "Completed" state, the client
 * transaction MUST move to the "Terminated" state.
 * <li>Non-Invite Client transaction: If the transaction times out while the
 * client transaction is still in the "Trying" or "Proceeding" state, the client
 * transaction SHOULD inform the application about the timeout, and then it
 * SHOULD enter the "Terminated" state. If the response retransmissions buffer
 * expires while in the "Completed" state, the client transaction MUST
 * transition to the "Terminated" state.
 * <li>Invite Server transaction: If in the "Proceeding" state, and the
 * application passes a 2xx response to the server transaction, the server
 * transaction MUST transition to the "Terminated" state. When the server
 * transaction abandons retransmitting the response while in the "Completed"
 * state, it implies that the ACK was never received. In this case, the server
 * transaction MUST transition to the "Terminated" state, and MUST indicate to
 * the TU that a transaction failure has occurred.
 * <li>Non-Invite Server transaction: If the request retransmissions buffer
 * expires while in the "Completed" state, the server transaction MUST
 * transition to the "Terminated" state.
 * </ul>
 * </ul>
 * 
 * For each specific transaction state machine, refer to
 * <a href = "http://www.ietf.org/rfc/rfc3261.txt">RFC3261</a>.
 */
public final class TransactionState implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final int SIZE = 6;
	private static final TransactionState[] TRANSACTION_STATE_ARRAY = new TransactionState[SIZE];

	private int transactionStateValue;

	/**
	 * This constant value indicates the internal value of the "Calling" constant.
	 * <br>
	 * This constant has an integer value of 0.
	 */
	public static final int CALLING_VALUE = 0;

	/**
	 * This constant value indicates that the transaction state is "Calling".
	 */
	public static final TransactionState CALLING = new TransactionState(CALLING_VALUE);

	/**
	 * This constant value indicates the internal value of the "Trying" constant.
	 * This constant has an integer value of 1.
	 */
	public static final int TRYING_VALUE = 1;
	/**
	 * This constant value indicates that the transaction state is "Trying".
	 */
	public static final TransactionState TRYING = new TransactionState(TRYING_VALUE);

	/**
	 * This constant value indicates the internal value of the "Proceeding"
	 * constant. <br>
	 * This constant has an integer value of 2.
	 */
	public static final int PROCEEDING_VALUE = 2;

	/**
	 * This constant value indicates that the transaction state is "Proceeding".
	 */
	public static final TransactionState PROCEEDING = new TransactionState(PROCEEDING_VALUE);

	/**
	 * This constant value indicates the internal value of the "Completed" constant.
	 * <br>
	 * This constant has an integer value of 3.
	 */
	public static final int COMPLETED_VALUE = 3;

	/**
	 * This constant value indicates that the transaction state is "Completed".
	 */
	public static final TransactionState COMPLETED = new TransactionState(COMPLETED_VALUE);

	/**
	 * This constant value indicates the internal value of the "Confirmed" constant.
	 * <br>
	 * This constant has an integer value of 4.
	 */
	public static final int CONFIRMED_VALUE = 4;

	/**
	 * This constant value indicates that the transaction state is "Confirmed".
	 */
	public static final TransactionState CONFIRMED = new TransactionState(CONFIRMED_VALUE);

	/**
	 * This constant value indicates the internal value of the "Terminated"
	 * constant. <br>
	 * This constant has an integer value of 5.
	 */
	public static final int TERMINATED_VALUE = 5;

	/**
	 * This constant value indicates that the transaction state is "Terminated".
	 */
	public static final TransactionState TERMINATED = new TransactionState(TERMINATED_VALUE);

	private static void setTransactionState(int transactionStateValue, TransactionState transactionState) {
		TRANSACTION_STATE_ARRAY[transactionStateValue] = transactionState;
	}

	/**
	 * 
	 * Constructor for the TransactionState
	 *
	 * 
	 * 
	 * @param transactionState The integer value for the TransactionState
	 * 
	 */
	private TransactionState(int transactionStateValue) {
		this.transactionStateValue = transactionStateValue;

		setTransactionState(transactionStateValue, this);
	}

	/**
	 * 
	 * This method returns the object value of the TransactionState
	 *
	 * @return The TransactionState Object
	 * 
	 * @param transactionState The integer value of the TransactionState
	 */
	public static TransactionState getObject(int transactionState) {
		if(transactionState >= 0 && transactionState < SIZE) {
			return TRANSACTION_STATE_ARRAY[transactionState];
		}

		throw new IllegalArgumentException("Invalid transactionState value");
	}

	/**
	 * 
	 * This method returns the integer value of the TransactionState
	 *
	 * 
	 * 
	 * @return The integer value of the TransactionState
	 * 
	 */

	public int getValue() {

		return transactionStateValue;

	}

	/**
	 * Returns the designated type as an alternative object to be used when writing an object to a stream. This method
	 * would be used when for example serializing TransactionState.EARLY and deserializing it afterwards results again
	 * in TransactionState.EARLY. If you do not implement readResolve(), you would not get TransactionState.EARLY but
	 * an instance with similar content.
	 *
	 * @return the TransactionState
	 * @exception ObjectStreamException
	 */
	private Object readResolve() throws ObjectStreamException {
		return TRANSACTION_STATE_ARRAY[transactionStateValue];
	}

	/**
	 * Compare this transaction state for equality with another.
	 * 
	 * @param obj the object to compare this with.
	 * @return <code>true</code> if <code>obj</code> is an instance of this class
	 *         representing the same transaction state as this, <code>false</code>
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		return (obj instanceof TransactionState) && ((TransactionState) obj).transactionStateValue == transactionStateValue;
	}

	/**
	 * Get a hash code value for this transaction state.
	 * 
	 * @return a hash code value.
	 */
	@Override
	public int hashCode() {
		return transactionStateValue;
	}

	/*
	 * This method returns a string version of this class.
	 * 
	 * @return The string version of the TransactionState
	 */
	@Override
	public String toString() {
		String text = "";

		switch(transactionStateValue) {
		case CALLING_VALUE:
			text = "Calling Transaction";
			break;
		case TRYING_VALUE:
			text = "Trying Transaction";
			break;
		case PROCEEDING_VALUE:
			text = "Proceeding Transaction";
			break;
		case COMPLETED_VALUE:
			text = "Completed Transaction";
			break;
		case CONFIRMED_VALUE:
			text = "Confirmed Transaction";
			break;
		case TERMINATED_VALUE:
			text = "Terminated Transaction";
			break;
		default:
			text = "Error while printing Transaction State";
			break;
		}

		return text;
	}
}
