package javax.sip;

/**
 * This exception class is thrown by an implementation when given an invalid
 * argument such as a invalid numerical value.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public class InvalidArgumentException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Create an <code>InvalidArgumentException</code> with no detail message.
	 */
	public InvalidArgumentException() {
		// nothing
	}

	/**
	 * Create an <code>InvalidArgumentException</code> with a detail message.
	 *
	 * @param message the detail message.
	 */
	public InvalidArgumentException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>InvalidArgumentException</code> with the specified
	 * error message and specialized cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public InvalidArgumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
