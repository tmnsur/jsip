package javax.sip;

/**
 * This Exception is thrown when a user attempts to reference Dialog that does
 * currently not exist in the underlying SipProvider
 */
public class DialogDoesNotExistException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>DialogDoesNotExistException</code>
	 */
	public DialogDoesNotExistException() {
		super();
	}

	/**
	 * Constructs a new <code>DialogDoesNotExistException</code> with the specified
	 * error message.
	 *
	 * @param message the detail of the error message
	 */
	public DialogDoesNotExistException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>DialogDoesNotExistException</code> with the specified
	 * error message and specialized cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public DialogDoesNotExistException(String message, Throwable cause) {
		super(message, cause);
	}
}
