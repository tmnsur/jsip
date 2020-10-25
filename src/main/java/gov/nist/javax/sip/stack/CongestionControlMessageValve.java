package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.SipStack;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * This is just a simple reusable congestion control valve JSIP apps can use to
 * stop traffic when the number of server transactions reaches the limit
 * specified in gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS without breaking
 * existing dialogs.
 * 
 * The drop policy is specified in DROP_RESPONSE_STATUS where "0" means silent
 * drop and any positive number will be interpreted as the status code of the
 * error response that will be generated.
 * 
 * To enable this in your application you must specify this property:
 * gov.nist.javax.sip.SIP_MESSAGE_VALVE=gov.nist.javax.sip.stack.CongestionControlMessageValve
 * 
 * It is advised to extend this class to add your application-specific control conditions
 */
public class CongestionControlMessageValve implements SIPMessageValve {
	private static final Logger logger = Logger.getLogger(CongestionControlMessageValve.class.getName());

	protected SipStackImpl sipStack;
	// High water mark for ServerTransaction Table after which requests are dropped.
	protected int serverTransactionTableHighwaterMark;
	protected int dropResponseStatus;

	@Override
	public boolean processRequest(SIPRequest request, MessageChannel messageChannel) {
		String requestMethod = request.getMethod();

		// We should not attempt to drop these requests because they actually free
		// resources
		// which is our goal in congested mode
		boolean undropableMethod = requestMethod.equals(Request.BYE) || requestMethod.equals(Request.ACK)
				|| requestMethod.equals(Request.PRACK) || requestMethod.equals(Request.CANCEL);

		if(!undropableMethod && serverTransactionTableHighwaterMark <= sipStack.getServerTransactionTableSize()) {
			// Allow directly any subsequent requests
			if(request.getToTag() != null) {
				return true;
			}

			if(dropResponseStatus > 0) {
				SIPResponse response = request.createResponse(dropResponseStatus);
				try {
					messageChannel.sendMessage(response);
				} catch(IOException e) {
					logger.log(Level.SEVERE, e, () -> MessageFormat.format("Failed to send congestion"
							+ " control error response {0}", response));
				}
			}

			// Do not pass this request to the pipeline
			return false;
		}

		// OK, the processing of the request can continue
		return true;
	}

	@Override
	public boolean processResponse(Response response, MessageChannel messageChannel) {
		return true;
	}

	@Override
	public void destroy() {
		logger.log(Level.INFO, "Destorying the congestion control valve {0}", this);
	}

	@Override
	public void init(SipStack stack) {
		sipStack = (SipStackImpl) stack;

		logger.log(Level.INFO, "Initializing congestion control valve");

		String serverTransactionsString = sipStack.getConfigurationProperties()
				.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS", "10000");
		serverTransactionTableHighwaterMark = Integer.valueOf(serverTransactionsString);

		String dropResponseStatusString = sipStack.getConfigurationProperties()
				.getProperty("DROP_RESPONSE_STATUS", "503");

		dropResponseStatus = Integer.valueOf(dropResponseStatusString);
	}
}
