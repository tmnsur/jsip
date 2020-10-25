package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.Require;
import gov.nist.javax.sip.header.RequireList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderNames;

/**
 * Parser for Require header.
 */
public class RequireParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(RequireParser.class.getName());

	/**
	 * Creates a new instance of RequireParser
	 * 
	 * @param require the header to parse
	 */
	public RequireParser(String require) {
		super(require);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected RequireParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (RequireList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(RequireParser.class.getName(), "parse");

		RequireList requireList = new RequireList();

		headerName(TokenTypes.REQUIRE);

		while (lexer.lookAhead(0) != '\n') {
			Require r = new Require();

			r.setHeaderName(SIPHeaderNames.REQUIRE);

			// Parsing the option tag
			this.lexer.match(TokenTypes.ID);

			Token token = lexer.getNextToken();

			r.setOptionTag(token.getTokenValue());

			this.lexer.SPorHT();

			requireList.add(r);

			while(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				r = new Require();

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				r.setOptionTag(token.getTokenValue());
				this.lexer.SPorHT();

				requireList.add(r);
			}
		}

		logger.exiting(RequireParser.class.getName(), "parse", requireList);

		return requireList;
	}
}
