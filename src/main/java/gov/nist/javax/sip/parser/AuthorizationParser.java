package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for authorization headers.
 */
public class AuthorizationParser extends ChallengeParser {
	private static final Logger logger = Logger.getLogger(AuthorizationParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param authorization Authorization message to parse
	 */
	public AuthorizationParser(String authorization) {
		super(authorization);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected AuthorizationParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (Authorization object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AuthorizationParser.class.getName(), "parse");

		headerName(TokenTypes.AUTHORIZATION);

		Authorization auth = new Authorization();

		super.parse(auth);

		logger.exiting(AuthorizationParser.class.getName(), "parse", auth);

		return auth;
	}
}
