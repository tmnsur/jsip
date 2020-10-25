package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for SIP-ETag header.
 */
public class SIPETagParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SIPETagParser.class.getName());

	/**
	 * Creates a new instance of PriorityParser
	 * 
	 * @param etag the header to parse
	 */
	public SIPETagParser(String etag) {
		super(etag);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected SIPETagParser(Lexer lexer) {
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
		logger.entering(SIPETagParser.class.getName(), "parse");

		SIPETag sipEtag = new SIPETag();
		headerName(TokenTypes.SIP_ETAG);

		this.lexer.SPorHT();
		this.lexer.match(TokenTypes.ID);
		Token token = lexer.getNextToken();

		sipEtag.setETag(token.getTokenValue());

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(SIPETagParser.class.getName(), "parse", sipEtag);

		return sipEtag;
	}
}
