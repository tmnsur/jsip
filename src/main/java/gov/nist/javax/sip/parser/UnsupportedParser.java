package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderNames;
import gov.nist.javax.sip.header.Unsupported;
import gov.nist.javax.sip.header.UnsupportedList;

/**
 * Parser for Unsupported header.
 */
public class UnsupportedParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(UnsupportedParser.class.getName());

	/**
	 * Creates a new instance of UnsupportedParser
	 * 
	 * @param unsupported - Unsupported header to parse
	 */
	public UnsupportedParser(String unsupported) {
		super(unsupported);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer - the lexer to use to parse the header
	 */
	protected UnsupportedParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (Unsupported object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(UnsupportedParser.class.getName(), "parse");

		UnsupportedList unsupportedList = new UnsupportedList();

		headerName(TokenTypes.UNSUPPORTED);

		while(lexer.lookAhead(0) != '\n') {
			this.lexer.SPorHT();
			Unsupported unsupported = new Unsupported();
			unsupported.setHeaderName(SIPHeaderNames.UNSUPPORTED);

			// Parsing the option tag
			this.lexer.match(TokenTypes.ID);

			Token token = lexer.getNextToken();

			unsupported.setOptionTag(token.getTokenValue());
			this.lexer.SPorHT();

			unsupportedList.add(unsupported);

			while(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				unsupported = new Unsupported();

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);

				token = lexer.getNextToken();
				unsupported.setOptionTag(token.getTokenValue());

				this.lexer.SPorHT();

				unsupportedList.add(unsupported);
			}
		}

		logger.exiting(UnsupportedParser.class.getName(), "parse", unsupportedList);

		return unsupportedList;
	}
}
