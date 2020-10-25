import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;

import gov.nist.javax.sip.header.CSeq;

public class Main implements SipListener {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {
		Main main = new Main();

		main.run();
	}

	private void run() throws Exception {
		Properties properties = new Properties();

		properties.setProperty("javax.sip.STACK_NAME", "NISTv1.2");

		SipFactory.getInstance().setPathName("gov.nist");

		SipStack sipStack = SipFactory.getInstance().createSipStack(properties);
		ListeningPoint listeningPoint = sipStack.createListeningPoint("127.0.0.1", ListeningPoint.PORT_5060,
				ListeningPoint.UDP);
		SipProvider sipProvider = sipStack.createSipProvider(listeningPoint);

		sipProvider.addSipListener(this);
	}

	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		logger.log(Level.INFO, "processDialogTerminated: {0}", dialogTerminatedEvent);
	}

	public void processIOException(IOExceptionEvent ioExceptionEvent) {
		logger.log(Level.INFO, "processIOException: {0}", ioExceptionEvent);
	}

	public void processRequest(RequestEvent requestEvent) {
		logger.log(Level.INFO, "processRequest: {0}, request: {1}, CSeq: {2}",
				new Object[] { requestEvent, requestEvent.getRequest(), requestEvent.getRequest().getHeader("CSeq") });

		CSeq cSeq = (CSeq) requestEvent.getRequest().getHeader("CSeq");

		logger.log(Level.INFO, "cSeq.getHeaderValue(): {0}", cSeq.getHeaderValue());
	}

	public void processResponse(ResponseEvent responseEvent) {
		logger.log(Level.INFO, "processResponse: {0}", responseEvent);
	}

	public void processTimeout(TimeoutEvent timeoutEvent) {
		logger.log(Level.INFO, "processTimeout: {0}", timeoutEvent);
	}

	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		logger.log(Level.INFO, "processTransactionTerminated: {0}", transactionTerminatedEvent);
	}
}
