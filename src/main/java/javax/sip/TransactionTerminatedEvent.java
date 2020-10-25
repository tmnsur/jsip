package javax.sip;

import java.util.EventObject;

/**
 * 
 * TransactionTerminatedEvent is delivered to the Listener when the transaction
 * transitions to the terminated state. An implementation is expected to deliver
 * this event to the listener when it discards all internal book keeping records
 * for a given transaction - thereby allowing the Listener to unmap its own data
 * structures.
 */
public class TransactionTerminatedEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private boolean server;
	private ServerTransaction serverTransaction;
	private ClientTransaction clientTransaction;

	/**
	 * Constructs a TransactionTerminatedEvent to indicate a server retransmission
	 * or transaction timeout.
	 *
	 * @param source            - the source of TransactionTerminatedEvent (the
	 *                          SipProvider associated with the transaction).
	 * @param serverTransaction - the server transaction that timed out.
	 */
	public TransactionTerminatedEvent(Object source, ServerTransaction serverTransaction) {
		super(source);

		this.serverTransaction = serverTransaction;
		this.server = true;
	}

	/**
	 * Constructs a TransactionTerminatedEvent to indicate a client retransmission
	 * or transaction timeout.
	 *
	 * @param source            - source of TransactionTerminatedEvent (the
	 *                          SipProvider associated with the transaction).
	 * @param clientTransaction - the client transaction that timed out.
	 */
	public TransactionTerminatedEvent(Object source, ClientTransaction clientTransaction) {
		super(source);

		this.clientTransaction = clientTransaction;
		this.server = false;
	}

	/**
	 * Gets the server transaction associated with this TransactionTerminatedEvent.
	 *
	 * @return server transaction associated with this TransactionTerminatedEvent,
	 *         or null if this event is specific to a client transaction.
	 */
	public ServerTransaction getServerTransaction() {
		return serverTransaction;
	}

	/**
	 * Gets the client transaction associated with this TransactionTerminatedEvent.
	 *
	 * @return client transaction associated with this TransactionTerminatedEvent,
	 *         or null if this event is specific to a server transaction.
	 */
	public ClientTransaction getClientTransaction() {
		return clientTransaction;
	}

	/**
	 * Indicates if the transaction associated with this TransactionTerminatedEvent
	 * is a server transaction.
	 *
	 * @return returns true if a server transaction or false if a client
	 *         transaction.
	 */
	public boolean isServerTransaction() {
		return server;
	}
}
