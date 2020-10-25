package gov.nist.javax.sip;

import java.util.EventObject;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;

/**
 * 
 * DialogAckTimeoutEvent is delivered to the Listener when the dialog does not
 * receive or send an ACK.
 */
public class DialogTimeoutEvent extends EventObject {
	private static final long serialVersionUID = -2514000059989311925L;

	public enum Reason {
		AckNotReceived, AckNotSent, ReInviteTimeout, EarlyStateTimeout, CannotAcquireAckSemaphoreForOk
	}

	// internal variables
	private Dialog dialog = null;
	private Reason reason = null;
	private ClientTransaction reInviteTransaction = null;

	/**
	 * Constructs a DialogTerminatedEvent to indicate a dialog timeout.
	 *
	 * @param source - the source of TimeoutEvent.
	 * @param dialog - the dialog that timed out.
	 */
	public DialogTimeoutEvent(Object source, Dialog dialog, Reason reason) {
		super(source);

		this.dialog = dialog;
		this.reason = reason;
	}

	/**
	 * Set the re-INVITE client transaction. This method will be used by
	 * Implementations (not applications).
	 * 
	 * @since v1.2
	 */
	public void setClientTransaction(ClientTransaction clientTransaction) {
		this.reInviteTransaction = clientTransaction;
	}

	/**
	 * Gets the Dialog associated with the event. This enables application
	 * developers to access the dialog associated to this event.
	 * 
	 * @return the dialog associated with the response event or null if there is no
	 *         dialog.
	 * @since v1.2
	 */
	public Dialog getDialog() {
		return dialog;
	}

	/**
	 * The reason for the Dialog Timeout Event being delivered to the application.
	 * 
	 * @return the reason for the timeout event.
	 */
	public Reason getReason() {
		return reason;
	}

	/**
	 * Get the re-INVITE client transaction that could not be sent.
	 * 
	 * @return the re-INVITE client transaction that could not be sent.
	 */
	public ClientTransaction getClientTransaction() {
		return reInviteTransaction;
	}
}
