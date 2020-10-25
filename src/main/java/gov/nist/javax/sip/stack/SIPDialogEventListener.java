package gov.nist.javax.sip.stack;

import java.util.EventListener;

/**
 * Interface implemented by classes that want to be notified of asynchronous
 * dialog events.
 */
public interface SIPDialogEventListener extends EventListener {
	/**
	 * Invoked when an error has occurred with a dialog.
	 *
	 * @param dialogErrorEvent Error event.
	 */
	public void dialogErrorEvent(SIPDialogErrorEvent dialogErrorEvent);
}
