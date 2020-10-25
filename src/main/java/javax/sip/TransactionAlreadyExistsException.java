package javax.sip;

/**
 * This Exception is thrown when a user attempts to get a transaction to handle
 * a message when infact a transaction is already handling this message.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public class TransactionAlreadyExistsException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TransactionAlreadyExistsException</code>
	 */
	public TransactionAlreadyExistsException() {
		super();
	}

	/**
	 * Constructs a new <code>TransactionAlreadyExistsException</code> with the
	 * specified error message.
	 *
	 * @param message the detail of the error message
	 */
	public TransactionAlreadyExistsException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TransactionAlreadyExistsException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TransactionAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}
}
