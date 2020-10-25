package javax.sip;

/**
 * The TransactionUnavailableException indicates that a vendor's implementation
 * could not create a Transaction for some reason.
 */
public class TransactionUnavailableException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TransactionUnavailableException</code>.
	 */
	public TransactionUnavailableException() {
		super();
	}

	/**
	 * Constructs a new <code>TransactionUnavailableException</code> with the
	 * specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public TransactionUnavailableException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TransactionUnavailableException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TransactionUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
