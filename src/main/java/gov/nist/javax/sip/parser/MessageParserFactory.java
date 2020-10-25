package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.stack.SIPTransactionStack;

/**
 * Factory used to create message parser instances on behalf of the stack 
 * which are created when a new message comes from the network and is processed by the stack. 
 * This allows to plug other implementations of MessageParser than the ones 
 * provided by default with the jain sip stack
 */
public interface MessageParserFactory {
	/**
	 * Creates a Message Parser
	 * 
	 * @param stack the stack if any configuration or runtime information is needed
	 * @return the newly created MessageParser
	 */
	MessageParser createMessageParser(SIPTransactionStack stack);
}
