package gov.nist.javax.sip;

import java.util.EventObject;

import gov.nist.javax.sip.stack.SIPTransaction;

public class EventWrapper {
	protected EventObject sipEvent;
	protected SIPTransaction transaction;

	public EventWrapper(EventObject sipEvent, SIPTransaction transaction) {
		this.sipEvent = sipEvent;
		this.transaction = transaction;
	}
}
