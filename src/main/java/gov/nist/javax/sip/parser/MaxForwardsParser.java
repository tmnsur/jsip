package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.SIPHeader;

/**
 * Parser for Max Forwards Header.
 */
public class MaxForwardsParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(MaxForwardsParser.class.getName());

	public MaxForwardsParser(String contentLength) {
		super(contentLength);
	}

	protected MaxForwardsParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(MaxForwardsParser.class.getName(), "parse");

		try {
			MaxForwards contentLength = new MaxForwards();
			headerName(TokenTypes.MAX_FORWARDS);
			String number = this.lexer.number();
			contentLength.setMaxForwards(Integer.parseInt(number));
			this.lexer.SPorHT();
			this.lexer.match('\n');

			logger.exiting(MaxForwardsParser.class.getName(), "parse", contentLength);

			return contentLength;
		} catch(InvalidArgumentException | NumberFormatException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
