package javax.sip;

/**
 * This exception is thrown by a method that is unable to delete a specified
 * Object because the Object is still in use by the underlying implementation.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public class ObjectInUseException extends SipException {
	private static final long serialVersionUID = 1L;

	/** 
	 * Constructs a new <code>ObjectInUseException</code>.
	 */
	public ObjectInUseException() {
		super();
	}

	/**
	 * Constructs a new <code>ObjectInUseException</code> with the specified error message.
	 *
	 * @param message the detailed error message
	 */
	public ObjectInUseException(String message) {
		super(message);
	}

	/**
	 * Constructs a new <code>ObjectInUseException</code> with the specified error message and specialized cause that
	 * triggered this error condition.
	 * 
	 * @param message - the detail of the error message 
	 * @param cause   - the specialized cause that triggered this exception
	 */
	public ObjectInUseException(String message, Throwable cause) {
		super(message, cause);
	}
}
