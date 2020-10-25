package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Factory used to create message processor instances on behalf of the stack 
 * which are created when a new Listening point is created. 
 * This allows to plug other implementations of MessageProcessor than the ones 
 * provided by default with the jain sip stack
 */
public interface MessageProcessorFactory {
	MessageProcessor createMessageProcessor(SIPTransactionStack sipStack, InetAddress ipAddress, int port,
			String transport) throws IOException; 
}
