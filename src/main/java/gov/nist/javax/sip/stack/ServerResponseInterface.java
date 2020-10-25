package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.message.*;

/**
 * An interface for a genereic message processor for SIP Response messages. This
 * is implemented by the application. The stack calls the message factory with a
 * pointer to the parsed structure to create one of these and then calls
 * processResponse on the newly created SIPServerResponse It is the applications
 * responsibility to take care of what needs to be done to actually process the
 * response.
 */
public interface ServerResponseInterface {
	/**
	 * Process the Response.
	 * 
	 * @param incomingChannel is the incoming message channel
	 * @param sipResponse     is the responseto process.
	 * @param sipDialog       -- dialog for this response
	 */
	public void processResponse(SIPResponse sipResponse, MessageChannel incomingChannel, SIPDialog sipDialog);

	/**
	 * This method is called prior to dialog assignment.
	 * 
	 * @param sipResponse
	 * @param incomingChannel
	 */
	public void processResponse(SIPResponse sipResponse, MessageChannel incomingChannel);
}
