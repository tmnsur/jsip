package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for MinExpires header.
 */
public class MinExpiresParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(MinExpiresParser.class.getName());

	/**
	 * Creates a new instance of MinExpiresParser
	 * 
	 * @param minExpires the header to parse
	 */
	public MinExpiresParser(String minExpires) {
		super(minExpires);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected MinExpiresParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (MinExpiresParser)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(MinExpiresParser.class.getName(), "parse");

		MinExpires minExpires = new MinExpires();

		headerName(TokenTypes.MIN_EXPIRES);

		minExpires.setHeaderName(SIPHeaderNames.MIN_EXPIRES);

		String number = this.lexer.number();
		try {
			long delta = Long.parseLong(number);
			if (delta > 4294967295L) {
				throw createParseException("bad integer format");
			}
			minExpires.setExpires(delta);
		} catch (InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}
		this.lexer.SPorHT();

		this.lexer.match('\n');

		logger.exiting(MinExpiresParser.class.getName(), "parse", minExpires);

		return minExpires;
	}
}
