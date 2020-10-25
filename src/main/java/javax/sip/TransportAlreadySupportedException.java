package javax.sip;

/**
 * The TransportAlreadySupportedException indicates that a specific transport is
 * already supported by a SipProvider via its ListeningPoints.
 */
public class TransportAlreadySupportedException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>TransportAlreadySupportedException</code>.
	 */
	public TransportAlreadySupportedException() {
		super();
	}

	/**
	 * Constructs a new <code>TransportAlreadySupportedException</code> with the
	 * specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public TransportAlreadySupportedException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>TransportAlreadySupportedException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public TransportAlreadySupportedException(String message, Throwable cause) {
		super(message, cause);
	}
}
