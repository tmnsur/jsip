package javax.sip;

/**
 * The PeerUnavailableException indicates that a vendor's implementation of a
 * JAIN SIP interface could not be created for some reason.
 */
public class PeerUnavailableException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>PeerUnavailableException</code>.
	 */
	public PeerUnavailableException() {
		super();
	}

	/**
	 * Constructs a new <code>PeerUnavailableException</code> with the specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public PeerUnavailableException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>PeerUnavailableException</code> with the specified
	 * error message and specialized cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public PeerUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
