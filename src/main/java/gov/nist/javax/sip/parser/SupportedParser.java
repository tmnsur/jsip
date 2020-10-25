package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Supported header.
 */
public class SupportedParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SupportedParser.class.getName());

	/**
	 * Creates a new instance of SupportedParser
	 * 
	 * @param supported the header to parse
	 */
	public SupportedParser(String supported) {
		super(supported);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected SupportedParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (Supported object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SupportedParser.class.getName(), "parse");

		SupportedList supportedList = new SupportedList();

		Token token;

		headerName(TokenTypes.SUPPORTED);

		this.lexer.SPorHT();
		Supported supported = new Supported();
		supported.setHeaderName(SIPHeaderNames.SUPPORTED);

		if (lexer.lookAhead(0) != '\n') {
			// Parsing the option tag
			this.lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			supported.setOptionTag(token.getTokenValue());
			this.lexer.SPorHT();
		}

		supportedList.add(supported);

		while (lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			supported = new Supported();

			// Parsing the option tag
			this.lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			supported.setOptionTag(token.getTokenValue());
			this.lexer.SPorHT();

			supportedList.add(supported);
		}

		logger.exiting(SupportedParser.class.getName(), "parse", supportedList);

		return supportedList;
	}
}
