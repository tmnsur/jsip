package javax.sip;

import java.util.EventObject;

/**
 * 
 * DialogTerminatedEvent is delivered to the Listener when the dialog
 * transitions to the terminated state. An implementation is expected to deliver
 * this event to the listener when it discards all internal book keeping records
 * for a given dialog, allowing the Listener to unmap its own data structures.
 */
public class DialogTerminatedEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private Dialog dialog = null;

	/**
	 * Constructs a DialogTerminatedEvent to indicate a dialog timeout.
	 *
	 * @param source - the source of TimeoutEvent.
	 * @param dialog - the dialog that timed out.
	 */
	public DialogTerminatedEvent(Object source, Dialog dialog) {
		super(source);

		this.dialog = dialog;
	}

	/**
	 * Gets the Dialog associated with the event. This enables application
	 * developers to access the dialog associated to this event.
	 * 
	 * @return the dialog associated with the response event or null if there is no
	 *         dialog.
	 */
	public Dialog getDialog() {
		return dialog;
	}
}
