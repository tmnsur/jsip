package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for SIP-If-Match header.
 */
public class SIPIfMatchParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SIPIfMatchParser.class.getName());

	/**
	 * Creates a new instance of PriorityParser
	 * 
	 * @param etag the header to parse
	 */
	public SIPIfMatchParser(String etag) {
		super(etag);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected SIPIfMatchParser(Lexer lexer) {
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
		logger.entering(SIPIfMatchParser.class.getName(), "parse");

		SIPIfMatch sipIfMatch = new SIPIfMatch();

		headerName(TokenTypes.SIP_IF_MATCH);

		this.lexer.SPorHT();
		this.lexer.match(TokenTypes.ID);
		Token token = lexer.getNextToken();

		sipIfMatch.setETag(token.getTokenValue());

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(SIPIfMatchParser.class.getName(), "parse", sipIfMatch);

		return sipIfMatch;
	}
}
