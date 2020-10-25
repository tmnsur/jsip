package gov.nist.javax.sip.parser;

import java.text.ParseException;

import gov.nist.javax.sip.message.SIPMessage;

/**
 * Interface defining the contract for the stack to interact with the message parser to parse a byte array containing
 * the SIP Message into a SIPMessage object
 */
public interface MessageParser {
	/**
	 * parse a byte array containing the SIP Message into a SIPMessage object
	 * 
	 * @param msgBytes the SIP Message received from the network
	 * @param readBody If the content body should be read or not
	 * @param exhandler Callback if an exception occurs during the parsing to notify back the stack 
	 * @return a SIPMessage object that the stack can interact with
	 * @throws ParseException if a parseexception occurs
	 */
	SIPMessage parseSIPMessage(byte[] msgBytes, boolean readBody, boolean strict, ParseExceptionListener exhandler)
			throws ParseException;
}
