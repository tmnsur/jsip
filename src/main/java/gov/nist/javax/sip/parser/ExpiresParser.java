package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for SIP Expires Parser. Converts from SIP Date to the internal storage
 * (Calendar).
 */
public class ExpiresParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ExpiresParser.class.getName());

	/**
	 * protected constructor.
	 * 
	 * @param text is the text of the header to parse
	 */
	public ExpiresParser(String text) {
		super(text);
	}

	/**
	 * constructor.
	 * 
	 * @param lexer is the lexer passed in from the enclosing parser.
	 */
	protected ExpiresParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Parse the header.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ExpiresParser.class.getName(), "parse");

		Expires expires = new Expires();

		lexer.match(TokenTypes.EXPIRES);
		lexer.SPorHT();
		lexer.match(':');
		lexer.SPorHT();

		String nextId = lexer.getNextId();
		try {
			long delta = Long.parseLong(nextId);

			if(delta > 4294967295L) {
				throw createParseException("bad integer format");
			}

			expires.setExpires(delta);

			this.lexer.match('\n');

			logger.exiting(ExpiresParser.class.getName(), "parse", expires);

			return expires;
		} catch (NumberFormatException ex) {
			throw createParseException("bad integer format");
		} catch (InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
