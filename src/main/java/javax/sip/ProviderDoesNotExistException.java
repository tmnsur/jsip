package javax.sip;

/**
 * This Exception is thrown when a user attempts to start the SipStack without
 * any SipProviders created to service requests and responses.
 */
public class ProviderDoesNotExistException extends SipException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>ProviderDoesNotExistException</code>
	 */
	public ProviderDoesNotExistException() {
		super();
	}

	/**
	 * Constructs a new <code>ProviderDoesNotExistException</code> with the specified error message.
	 * 
	 * @param message the detail of the error message
	 */
	public ProviderDoesNotExistException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>ProviderDoesNotExistException</code> with the specified error message and specialized
	 * cause that triggered this error condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public ProviderDoesNotExistException(String message, Throwable cause) {
		super(message, cause);
	}
}
