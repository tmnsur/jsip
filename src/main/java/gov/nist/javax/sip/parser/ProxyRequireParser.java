package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for ProxyRequire header.
 */
public class ProxyRequireParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ProxyRequireParser.class.getName());

	/**
	 * Creates a new instance of ProxyRequireParser
	 * 
	 * @param require the header to parse
	 */
	public ProxyRequireParser(String require) {
		super(require);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ProxyRequireParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (ProxyRequireList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ProxyRequireParser.class.getName(), "parse");

		ProxyRequireList list = new ProxyRequireList();

		headerName(TokenTypes.PROXY_REQUIRE);

		while (lexer.lookAhead(0) != '\n') {
			ProxyRequire r = new ProxyRequire();
			r.setHeaderName(SIPHeaderNames.PROXY_REQUIRE);

			// Parsing the option tag
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			r.setOptionTag(token.getTokenValue());
			this.lexer.SPorHT();

			list.add(r);

			while(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				r = new ProxyRequire();

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				r.setOptionTag(token.getTokenValue());
				this.lexer.SPorHT();

				list.add(r);
			}
		}

		logger.exiting(ProxyRequireParser.class.getName(), "parse", list);

		return list;
	}
}
