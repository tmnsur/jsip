package javax.sip;

/**
 * This Exception is thrown when a user attempts to reference a client or server
 * transaction that does currently not exist in the underlying SipProvider
 */
public class TransactionDoesNotExistException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TransactionDoesNotExistException</code>
	 */
	public TransactionDoesNotExistException() {
		super();
	}

	/**
	 * Constructs a new <code>TransactionDoesNotExistException</code> with the
	 * specified error message.
	 *
	 * @param message the detail of the error message
	 */
	public TransactionDoesNotExistException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TransactionDoesNotExistException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TransactionDoesNotExistException(String message, Throwable cause) {
		super(message, cause);
	}
}
