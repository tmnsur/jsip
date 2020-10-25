package javax.sip;

/**
 * A SipException is thrown when a general SIP exception is encountered, when no
 * other specialized exception defined in this specification can handle the
 * error.
 */
public class SipException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>SipException</code>
	 */
	public SipException() {
		super();
	}

	/**
	 * Constructs a new <code>SipException</code> with the specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public SipException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>SipException</code> with the specified error message
	 * and specialized cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public SipException(String message, Throwable cause) {
		super(message, cause);
	}
}
