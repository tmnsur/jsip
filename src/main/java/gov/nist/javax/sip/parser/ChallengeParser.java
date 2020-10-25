package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AuthenticationHeader;

/**
 * Parser for the challenge portion of the authentication header.
 */
public abstract class ChallengeParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ChallengeParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param String challenge message to parse to set
	 */
	protected ChallengeParser(String challenge) {
		super(challenge);
	}

	/**
	 * Constructor
	 * 
	 * @param String challenge message to parse to set
	 */
	protected ChallengeParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Get the parameter of the challenge string
	 * 
	 * @return NameValue containing the parameter
	 */
	protected void parseParameter(AuthenticationHeader header) throws ParseException {
		logger.entering(ChallengeParser.class.getName(), "parseParameter", header);

		header.setParameter(this.nameValue('='));

		logger.exiting(ChallengeParser.class.getName(), "parseParameter");
	}

	/**
	 * parser the String message.
	 * 
	 * @param header - header structure to fill in.
	 * @throws ParseException if the message does not respect the spec.
	 */
	public void parse(AuthenticationHeader header) throws ParseException {
		// the Scheme:
		this.lexer.SPorHT();

		lexer.match(TokenTypes.ID);

		Token type = lexer.getNextToken();

		this.lexer.SPorHT();

		header.setScheme(type.getTokenValue());

		// The parameters:
		while(lexer.lookAhead(0) != '\n') {
			this.parseParameter(header);
			this.lexer.SPorHT();

			char la = lexer.lookAhead(0);
			if(la == '\n' || la == '\0') {
				break;
			}

			this.lexer.match(',');
			this.lexer.SPorHT();
		}
	}
}
