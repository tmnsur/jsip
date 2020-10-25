package gov.nist.javax.sip;

import javax.sip.Dialog;
import javax.sip.SipListener;

/**
 * This interface extends the {@link SipListener} interface and adds the
 * following events to it :
 * <ul>
 * <li>{@link DialogTimeoutEvent}- these are timeout notifications emitted as
 * events by the SipProvider. Timeout events represent timers expiring in the
 * underlying SipProvider dialog state machine. These timeout's events notify
 * the application that a dialog has timed out.</li>
 * </ul>
 */
public interface SipListenerExt extends SipListener {
	/**
	 * Processes an expiration Timeout of an underlying {@link Dialog} handled by
	 * this SipListener. This Event notifies the application that a dialog Timer
	 * expired in the Dialog's state machine. Such a condition can occur when the
	 * application fails to send an ACK after receiving an OK response or if an ACK
	 * is not received after an OK is sent. The DialogTimeoutEvent encapsulates the
	 * specific timeout type and the dialog identifier. The type of Timeout can by
	 * determined by:
	 * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
	 * 
	 * Applications implementing this method should take care of sending the BYE or
	 * terminating the dialog to avoid any dialog leaks.
	 * 
	 * @param timeoutEvent - the timeoutEvent received indicating the dialog timed
	 *                     out.
	 */
	public void processDialogTimeout(DialogTimeoutEvent timeoutEvent);
}
