package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.*;
import java.util.logging.Logger;

/**
 * Parser for CALL ID header.
 */
public class CallIDParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(CallIDParser.class.getName());

	/**
	 * Creates new CallIDParser
	 * 
	 * @param callID message to parse
	 */
	public CallIDParser(String callID) {
		super(callID);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected CallIDParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (CallID object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(CallIDParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.CALL_ID);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		CallID callID = new CallID();

		this.lexer.SPorHT();
		String rest = lexer.getRest();

		callID.setCallId(rest.trim());

		logger.entering(CallIDParser.class.getName(), "parse", callID);

		return callID;
	}
}
