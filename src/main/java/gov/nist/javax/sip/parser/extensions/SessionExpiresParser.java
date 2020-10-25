package gov.nist.javax.sip.parser.extensions;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * Parser for SIP Session Expires Header.
 */
public class SessionExpiresParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(SessionExpiresParser.class.getName());

	/**
	 * protected constructor.
	 * 
	 * @param text is the text of the header to parse
	 */
	public SessionExpiresParser(String text) {
		super(text);
	}

	/**
	 * constructor.
	 * 
	 * @param lexer is the lexer passed in from the enclosing parser.
	 */
	protected SessionExpiresParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Parse the header.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		SessionExpires se = new SessionExpires();

		logger.entering(SessionExpiresParser.class.getName(), "parse");

		try {
			headerName(TokenTypes.SESSIONEXPIRES_TO);

			String nextId = lexer.getNextId();

			try {
				int delta = Integer.parseInt(nextId);
				se.setExpires(delta);
			} catch(NumberFormatException ex) {
				throw createParseException("bad integer format");
			} catch(InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}

			// May have parameters...

			this.lexer.SPorHT();

			super.parse(se);

			return se;
		} finally {
			logger.exiting(SessionExpiresParser.class.getName(), "parse");
		}
	}
}
