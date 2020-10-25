package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for TimeStamp header.
 */
public class TimeStampParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(TimeStampParser.class.getName());

	/**
	 * Creates a new instance of TimeStampParser
	 * 
	 * @param timeStamp the header to parse
	 */
	public TimeStampParser(String timeStamp) {
		super(timeStamp);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected TimeStampParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (TimeStamp object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(TimeStampParser.class.getName(), "parse");

		TimeStamp timeStamp = new TimeStamp();

		headerName(TokenTypes.TIMESTAMP);

		timeStamp.setHeaderName(SIPHeaderNames.TIMESTAMP);

		this.lexer.SPorHT();
		String firstNumber = this.lexer.number();

		try {

			if (lexer.lookAhead(0) == '.') {
				this.lexer.match('.');
				String secondNumber = this.lexer.number();

				String s = firstNumber + "." + secondNumber;
				float ts = Float.parseFloat(s);
				timeStamp.setTimeStamp(ts);
			} else {
				long ts = Long.parseLong(firstNumber);
				timeStamp.setTime(ts);
			}

		} catch (NumberFormatException | InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}

		this.lexer.SPorHT();
		if(lexer.lookAhead(0) != '\n') {
			firstNumber = this.lexer.number();

			try {
				if(lexer.lookAhead(0) == '.') {
					this.lexer.match('.');
					String secondNumber = this.lexer.number();

					String s = firstNumber + "." + secondNumber;

					timeStamp.setDelay(Float.parseFloat(s));
				} else {
					timeStamp.setDelay(Integer.parseInt(firstNumber));
				}

			} catch (NumberFormatException | InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}
		}

		logger.exiting(TimeStampParser.class.getName(), "parse", timeStamp);

		return timeStamp;
	}
}
