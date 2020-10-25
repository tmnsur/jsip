package gov.nist.javax.sip.stack;

import javax.sip.SipException;

public class IllegalTransactionStateException extends SipException {
	private static final long serialVersionUID = 1L;

	Reason reason = Reason.GenericReason;

	public enum Reason {
		RequestAlreadySent, MissingRequiredHeader, UnmatchingCSeq, ExpiresHeaderMandatory, ContactHeaderMandatory,
		GenericReason
	}

	/**
	 * Constructs a new <code>IllegalTransactionStateException</code>
	 */
	public IllegalTransactionStateException(Reason reason) {
		super();
		this.reason = reason;
	}

	/**
	 * Constructs a new <code>IllegalTransactionStateException</code> with the
	 * specified error message.
	 *
	 * @param message the error message of this Exception.
	 */
	public IllegalTransactionStateException(String message, Reason reason) {
		super(message);
		this.reason = reason;
	}

	/**
	 * Constructs a new <code>IllegalTransactionStateException</code> with the
	 * specified error message and specialized cause that triggered this error
	 * condition.
	 *
	 * @param message the detail of the error message
	 * @param cause   the specialized cause that triggered this exception
	 */
	public IllegalTransactionStateException(String message, Throwable cause, Reason reason) {
		super(message, cause);
		this.reason = reason;
	}

	/**
	 * Returns the reason of this exception
	 * 
	 * @return the reason of this exception
	 */
	public Reason getReason() {
		return (reason);
	}
}
