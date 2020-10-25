package gov.nist.javax.sip.parser;

import gov.nist.core.LexerCore;
import gov.nist.core.ParserCore;
import gov.nist.core.Token;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Base parser class.
 */
public abstract class Parser extends ParserCore implements TokenTypes {
	private static final Logger logger = Logger.getLogger(Parser.class.getName());

	protected ParseException createParseException(String exceptionString) {
		return new ParseException(lexer.getBuffer() + ":" + exceptionString, lexer.getPtr());
	}

	protected Lexer getLexer() {
		return (Lexer) this.lexer;
	}

	protected String sipVersion() throws ParseException {
		logger.entering(Parser.class.getName(), "sipVersion");

		Token tok = lexer.match(SIP);

		if(!tok.getTokenValue().equalsIgnoreCase("SIP")) {
			createParseException("Expecting SIP");
		}

		lexer.match('/');

		tok = lexer.match(ID);

		if(!tok.getTokenValue().equals("2.0")) {
			createParseException("Expecting SIP/2.0");
		}

		logger.exiting(Parser.class.getName(), "sipVersion", "SIP/2.0");

		return "SIP/2.0";
	}

	/**
	 * parses a method. Consumes if a valid method has been found.
	 */
	protected String method() throws ParseException {
		logger.entering(Parser.class.getName(), "method");

		Token[] tokens = this.lexer.peekNextToken(1);
		Token token = tokens[0];

		if(token.getTokenType() == INVITE || token.getTokenType() == ACK || token.getTokenType() == OPTIONS
				|| token.getTokenType() == BYE || token.getTokenType() == REGISTER || token.getTokenType() == CANCEL
				|| token.getTokenType() == SUBSCRIBE || token.getTokenType() == NOTIFY
				|| token.getTokenType() == PUBLISH || token.getTokenType() == MESSAGE
				|| token.getTokenType() == ID) {
			lexer.consume();

			String tokenValue = token.getTokenValue();

			logger.exiting(Parser.class.getName(), "method", tokenValue);

			return tokenValue;
		} else {
			throw createParseException("Invalid Method");
		}
	}

	/**
	 * Verifies that a given string matches the 'token' production in RFC3261
	 *
	 * @param token
	 * @throws ParseException - if there are invalid characters
	 */
	public static final void checkToken(String token) throws ParseException {
		if(token == null || token.length() == 0) {
			throw new ParseException("null or empty token", -1);
		}

		for(int i = 0; i < token.length(); ++i) {
			if(!LexerCore.isTokenChar(token.charAt(i))) {
				throw new ParseException("Invalid character(s) in string (not allowed in 'token')", i);
			}
		}
	}
}
