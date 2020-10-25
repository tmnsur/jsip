package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.Priority;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderNames;

/**
 * Parser for Priority header.
 */
public class PriorityParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(PriorityParser.class.getName());

	/**
	 * Creates a new instance of PriorityParser
	 * 
	 * @param priority the header to parse
	 */
	public PriorityParser(String priority) {
		super(priority);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected PriorityParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String header
	 * 
	 * @return SIPHeader (Priority object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PriorityParser.class.getName(), "parse");

		Priority priority = new Priority();

		headerName(TokenTypes.PRIORITY);

		priority.setHeaderName(SIPHeaderNames.PRIORITY);

		this.lexer.SPorHT();

		// This is in violation of the RFC but
		// let us be generous in what we accept.
		priority.setPriority(this.lexer.ttokenSafe());

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(PriorityParser.class.getName(), "parse", priority);

		return priority;
	}
}
