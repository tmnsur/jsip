package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.WWWAuthenticate;

/**
 * Parser for WWW authenticate header.
 */
public class WWWAuthenticateParser extends ChallengeParser {
	private static final Logger logger = Logger.getLogger(WWWAuthenticateParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param wwwAuthenticate - message to parse
	 */
	public WWWAuthenticateParser(String wwwAuthenticate) {
		super(wwwAuthenticate);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer - lexer to use.
	 */
	protected WWWAuthenticateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (WWWAuthenticate object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(WWWAuthenticateParser.class.getName(), "parse");

		headerName(TokenTypes.WWW_AUTHENTICATE);

		WWWAuthenticate wwwAuthenticate = new WWWAuthenticate();

		super.parse(wwwAuthenticate);

		logger.exiting(WWWAuthenticateParser.class.getName(), "parse", wwwAuthenticate);

		return wwwAuthenticate;
	}
}
