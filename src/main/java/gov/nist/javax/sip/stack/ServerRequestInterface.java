package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.message.*;

/**
 * An interface for a genereic message processor for SIP Request messages. This
 * is implemented by the application. The stack calls the message factory with a
 * pointer to the parsed structure to create one of these and then calls
 * processRequest on the newly created SIPServerRequest It is the applications
 * responsibility to take care of what needs to be done to actually process the
 * request.
 */
public interface ServerRequestInterface {
	/**
	 * Process the message. This incorporates a feature request by Salvador Rey
	 * Calatayud <salreyca@TELECO.UPV.ES>
	 * 
	 * @param sipRequest      is the incoming SIP Request.
	 * @param incomingChannel is the incoming message channel (parameter added in
	 *                        response to a request by Salvador Rey Calatayud.)
	 */
	public void processRequest(SIPRequest sipRequest, MessageChannel incomingChannel);
}
