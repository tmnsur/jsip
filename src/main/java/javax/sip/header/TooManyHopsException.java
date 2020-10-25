package javax.sip.header;

import javax.sip.SipException;

/**
 * This Exception is thrown when a user attempts decrement the Hop count when
 * the message as already reached its max number of forwards.
 */
public class TooManyHopsException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TooManyHopsException</code>
	 */
	public TooManyHopsException() {
		super();
	}

	/**
	 * Constructs a new <code>TooManyHopsException</code> with the specified error
	 * message.
	 *
	 * @param message the detail of the error message
	 */
	public TooManyHopsException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TooManyHopsException</code> with the specified error
	 * message and specialized cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TooManyHopsException(String message, Throwable cause) {
		super(message, cause);
	}
}
