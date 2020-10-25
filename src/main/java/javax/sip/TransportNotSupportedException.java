package javax.sip;

/**
 * The TransportNotSupportedException indicates that a specific transport is not
 * supported by a vendor's implementation of this specification.
 */
public class TransportNotSupportedException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TransportNotSupportedException</code>.
	 */
	public TransportNotSupportedException() {
		super();
	}

	/**
	 * Constructs a new <code>TransportNotSupportedException</code> with the
	 * specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public TransportNotSupportedException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TransportNotSupportedException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TransportNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}
}
