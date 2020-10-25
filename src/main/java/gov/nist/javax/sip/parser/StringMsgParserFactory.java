package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.stack.SIPTransactionStack;

/**
 * Default Message Parser Factory Implementation
 */
public class StringMsgParserFactory implements MessageParserFactory {
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.parser.MessageParserFactory#createMessageParser(gov.nist.javax.sip.stack.SIPTransactionStack)
	 */
	public MessageParser createMessageParser(SIPTransactionStack stack) {	
		return new StringMsgParser();
	}
}
