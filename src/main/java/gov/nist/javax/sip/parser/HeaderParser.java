package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.util.*;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * Generic header parser class. The parsers for various headers extend this
 * class. To create a parser for a new header, extend this class and change the
 * createParser class.
 */
public class HeaderParser extends Parser {
	private static final Logger logger = Logger.getLogger(HeaderParser.class.getName());

	/**
	 * Parse the weekday field
	 * 
	 * @return an integer with the calendar content for wkday.
	 */
	protected int wkday() throws ParseException {
		logger.entering(HeaderParser.class.getName(), "wkday");

		String tok = lexer.ttoken();
		String id = tok.toLowerCase();

		int result;
		if(TokenNames.MON.equalsIgnoreCase(id)) {
			result = Calendar.MONDAY;
		} else if (TokenNames.TUE.equalsIgnoreCase(id)) {
			result = Calendar.TUESDAY;
		} else if (TokenNames.WED.equalsIgnoreCase(id)) {
			result = Calendar.WEDNESDAY;
		} else if (TokenNames.THU.equalsIgnoreCase(id)) {
			result = Calendar.THURSDAY;
		} else if (TokenNames.FRI.equalsIgnoreCase(id)) {
			result = Calendar.FRIDAY;
		} else if (TokenNames.SAT.equalsIgnoreCase(id)) {
			result = Calendar.SATURDAY;
		} else if (TokenNames.SUN.equalsIgnoreCase(id)) {
			result = Calendar.SUNDAY;
		} else {
			throw createParseException("bad wkday");
		}

		logger.exiting(HeaderParser.class.getName(), "wkday", result);

		return result;
	}

	/**
	 * parse and return a date field.
	 * 
	 * @return a date structure with the parsed value.
	 */
	protected Calendar date() throws ParseException {
		try {
			Calendar retval = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

			String s1 = lexer.number();

			int day = Integer.parseInt(s1);

			if(day <= 0 || day > 31) {
				throw createParseException("Bad day ");
			}

			retval.set(Calendar.DAY_OF_MONTH, day);

			lexer.match(' ');

			String month = lexer.ttoken().toLowerCase();
			if(month.equals("jan")) {
				retval.set(Calendar.MONTH, Calendar.JANUARY);
			} else if (month.equals("feb")) {
				retval.set(Calendar.MONTH, Calendar.FEBRUARY);
			} else if (month.equals("mar")) {
				retval.set(Calendar.MONTH, Calendar.MARCH);
			} else if (month.equals("apr")) {
				retval.set(Calendar.MONTH, Calendar.APRIL);
			} else if (month.equals("may")) {
				retval.set(Calendar.MONTH, Calendar.MAY);
			} else if (month.equals("jun")) {
				retval.set(Calendar.MONTH, Calendar.JUNE);
			} else if (month.equals("jul")) {
				retval.set(Calendar.MONTH, Calendar.JULY);
			} else if (month.equals("aug")) {
				retval.set(Calendar.MONTH, Calendar.AUGUST);
			} else if (month.equals("sep")) {
				retval.set(Calendar.MONTH, Calendar.SEPTEMBER);
			} else if (month.equals("oct")) {
				retval.set(Calendar.MONTH, Calendar.OCTOBER);
			} else if (month.equals("nov")) {
				retval.set(Calendar.MONTH, Calendar.NOVEMBER);
			} else if (month.equals("dec")) {
				retval.set(Calendar.MONTH, Calendar.DECEMBER);
			}

			lexer.match(' ');

			String s2 = lexer.number();

			int yr = Integer.parseInt(s2);

			retval.set(Calendar.YEAR, yr);

			return retval;
		} catch (Exception ex) {
			throw createParseException("bad date field");
		}
	}

	/**
	 * Set the time field. This has the format hour:minute:second
	 */
	protected void time(Calendar calendar) throws ParseException {
		try {
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(lexer.number()));

			lexer.match(':');

			calendar.set(Calendar.MINUTE, Integer.parseInt(lexer.number()));

			lexer.match(':');

			calendar.set(Calendar.SECOND, Integer.parseInt(lexer.number()));
		} catch (Exception ex) {
			throw createParseException("error processing time ");
		}
	}

	/**
	 * Creates new HeaderParser
	 * 
	 * @param String to parse.
	 */
	protected HeaderParser(String header) {
		this.lexer = new Lexer("command_keywordLexer", header);
	}

	protected HeaderParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("command_keywordLexer");
	}

	/**
	 * Parse the SIP header from the buffer and return a parsed structure.
	 * 
	 * @throws ParseException if there was an error parsing.
	 */
	public SIPHeader parse() throws ParseException {
		String name = lexer.getNextToken(':');

		lexer.consume(1);

		String body = lexer.getLine().trim();

		// we don't set any fields because the header is ok
		ExtensionHeaderImpl retval = new ExtensionHeaderImpl(name);

		retval.setValue(body);

		return retval;
	}

	/**
	 * Parse the header name until the colon and chew WS after that.
	 */
	protected void headerName(int tok) throws ParseException {
		this.lexer.match(tok);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
	}
}
