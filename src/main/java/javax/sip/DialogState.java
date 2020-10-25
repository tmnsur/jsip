package javax.sip;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class contains the enumerations that define the underlying state of an
 * existing dialog.
 *
 * There are three explicit states for a dialog, namely:
 * <ul>
 * <li>Early - A dialog is in the "early" state, which occurs when it is created
 * when a provisional response is received to the INVITE Request.
 * <li>Confirmed - A dialog transitions to the "confirmed" state when a 2xx
 * final response is received to the INVITE Request.
 * <li>Terminated - A dialog transitions to the "terminated" state for all other
 * reasons or if no response arrives at all on the dialog.
 * </ul>
 */
public final class DialogState implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * This constant value indicates the internal value of the "Early" constant.
	 * <br>
	 * This constant has an integer value of 0.
	 */
	public static final int EARLY_VALUE = 0;

	/**
	 * This constant value indicates that the dialog state is "Early".
	 */
	public static final DialogState EARLY = new DialogState(EARLY_VALUE);

	/**
	 * This constant value indicates the internal value of the "Confirmed" constant.
	 * <br>
	 * This constant has an integer value of 1.
	 */
	public static final int CONFIRMED_VALUE = 1;

	/**
	 * This constant value indicates that the dialog state is "Confirmed".
	 */
	public static final DialogState CONFIRMED = new DialogState(CONFIRMED_VALUE);

	/**
	 * This constant value indicates the internal value of the "Completed" constant.
	 * <br>
	 * This constant has an integer value of 2.
	 * 
	 * @deprecated Since v1.2. This state does not exist in a dialog.
	 */
	public static final int COMPLETED_VALUE = 2;

	/**
	 * This constant value indicates that the dialog state is "Completed".
	 * 
	 * @deprecated Since v1.2. This state does not exist in a dialog.
	 */
	public static final DialogState COMPLETED = new DialogState(COMPLETED_VALUE);

	/**
	 * This constant value indicates the internal value of the "Terminated"
	 * constant. <br>
	 * This constant has an integer value of 3.
	 */
	public static final int TERMINATED_VALUE = 3;

	/**
	 * This constant value indicates that the dialog state is "Terminated".
	 */
	public static final DialogState TERMINATED = new DialogState(TERMINATED_VALUE);

	private static final int SIZE = 4;
	private static final DialogState[] DIALOG_STATE_ARRAY = new DialogState[SIZE];

	private int dialogStateValue;

	private static void setDialogState(int dialogStateValue, DialogState dialogState) {
		DIALOG_STATE_ARRAY[dialogStateValue] = dialogState;
	}

	/**
	 * Constructor for the DialogState
	 *
	 * @param dialogState The integer value for the DialogueState
	 */
	private DialogState(int dialogState) {
		this.dialogStateValue = dialogState;

		setDialogState(dialogStateValue, this);
	}

	/**
	 * This method returns the object value of the DialogState
	 *
	 * @return The DialogState Object
	 * @param dialogState The integer value of the DialogState
	 */
	public static DialogState getObject(int dialogState) {
		if(dialogState >= 0 && dialogState < SIZE) {
			return DIALOG_STATE_ARRAY[dialogState];
		}

		throw new IllegalArgumentException("Invalid dialogState value");
	}

	/**
	 * This method returns the integer value of the DialogState
	 *
	 * @return The integer value of the DialogState
	 */
	public int getValue() {
		return dialogStateValue;
	}

	/**
	 * Returns the designated type as an alternative object to be used when writing
	 * an object to a stream.
	 *
	 * This method would be used when for example serializing DialogState.EARLY and
	 * deserializing it afterwards results again in DialogState.EARLY. If you do not
	 * implement readResolve(), you would not get DialogState.EARLY but an instance
	 * with similar content.
	 *
	 * @return the DialogState
	 * @exception ObjectStreamException
	 */
	private Object readResolve() throws ObjectStreamException {
		return DIALOG_STATE_ARRAY[dialogStateValue];
	}

	/**
	 * Compare this dialog state for equality with another.
	 * 
	 * @param obj the object to compare this with.
	 * @return <code>true</code> if <code>obj</code> is an instance of this class
	 *         representing the same dialog state as this, <code>false</code>
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}

		return (obj instanceof DialogState) && ((DialogState) obj).dialogStateValue == dialogStateValue;
	}

	/**
	 * Get a hash code value for this dialog state.
	 * 
	 * @return a hash code value.
	 */
	@Override
	public int hashCode() {
		return dialogStateValue;
	}

	/**
	 * This method returns a string version of this class.
	 * 
	 * @return The string version of the DialogState
	 */
	@Override
	public String toString() {
		String text;
		switch(dialogStateValue) {
		case EARLY_VALUE:
			text = "Early Dialog";
			break;
		case CONFIRMED_VALUE:
			text = "Confirmed Dialog";
			break;
		case COMPLETED_VALUE:
			text = "Completed Dialog";
			break;
		case TERMINATED_VALUE:
			text = "Terminated Dialog";
			break;
		default:
			text = "Error while printing Dialog State";
			break;
		}

		return text;
	}
}
