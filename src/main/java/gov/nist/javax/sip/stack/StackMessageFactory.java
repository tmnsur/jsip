package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

/**
 * An interface for generating new requests and responses. This is implemented
 * by the application and called by the stack for processing requests and
 * responses. When a Request comes in off the wire, the stack calls
 * newSIPServerRequest which is then responsible for processing the request.
 * When a response comes off the wire, the stack calls newSIPServerResponse to
 * process the response.
 */
public interface StackMessageFactory {
	/**
	 * Make a new SIPServerResponse given a SIPRequest and a message channel.
	 *
	 * @param sipRequest        is the incoming request.
	 * @param serverTransaction is the message channel on which this request was
	 *                          received.
	 */
	public ServerRequestInterface newSIPServerRequest(SIPRequest sipRequest, SIPTransaction sipTransaction);

	/**
	 * Generate a new server response for the stack.
	 *
	 * @param sipResponse       is the incoming response.
	 * @param serverTransaction is the message channel on which the response was
	 *                          received.
	 */
	public ServerResponseInterface newSIPServerResponse(SIPResponse sipResponse, MessageChannel msgChannel);
}
