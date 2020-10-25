package javax.sip;

import java.util.EventObject;

/**
 * This object is used to signal to the application that an IO Exception has
 * occurred. The transaction state machine requires to report asynchronous IO
 * Exceptions to the application immediately (according to RFC 3261). This class
 * represents an IOExceptionEvent that is passed from a SipProvider to its
 * SipListener. This event enables an implementation to propagate the
 * asynchronous handling of IO Exceptions to the application. An application
 * (SipListener) will register with the SIP protocol stack (SipProvider) and
 * listen for IO Exceptions from the SipProvider. In many cases, when sending a
 * SIP message, the sending function will return before the message was actually
 * sent. This will happen for example if there is a need to wait for a response
 * from a DNS server or to perform other asynchronous actions such as connecting
 * a TCP connection. Later on if the message sending fails an IO exception event
 * will be given to the application. IO Exception events may also be reported
 * asynchronously when the Transaction State machine attempts to resend a
 * pending request. Note that synchronous IO Exceptions are presented to the
 * caller as SipException.
 */
public class IOExceptionEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private String host;
	private int port;
	private String transport;

	/**
	 * Constructor
	 *
	 * @param source     -- the object that is logically deemed to have caused the
	 *                   IO Exception (dialog/transaction/provider).
	 * @param remoteHost -- host where the request/response was heading
	 * @param port       -- port where the request/response was heading
	 * @param transport  -- transport ( i.e. UDP/TCP/TLS).
	 */
	public IOExceptionEvent(Object source, String remoteHost, int port, String transport) {
		super(source);

		this.host = remoteHost;
		this.port = port;
		this.transport = transport;
	}

	/**
	 * Return the host where Socket was pointing.
	 *
	 * @return host
	 *
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the port where the socket was trying to send amessage.
	 *
	 * @return port associated with the IOException
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Return transport used for the failed write attempt.
	 *
	 * @return the transaction associated with the IOException
	 */
	public String getTransport() {
		return this.transport;
	}
}
