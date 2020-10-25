package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.Calendar;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.header.SIPHeader;

/**
 * Parser for SIP Date field. Converts from SIP Date to the internal storage
 * (Calendar)
 */
public class DateParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(DateParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param date message to parse to set
	 */
	public DateParser(String date) {
		super(date);
	}

	protected DateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Parse method.
	 * 
	 * @throws ParseException
	 * @return the parsed Date header/
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(DateParser.class.getName(), "parse");

		headerName(TokenTypes.DATE);

		wkday();

		lexer.match(',');
		lexer.match(' ');

		Calendar cal = date();

		lexer.match(' ');
		time(cal);
		lexer.match(' ');

		String tzone = this.lexer.ttoken().toLowerCase();

		if(!"gmt".equals(tzone)) {
			throw createParseException("Bad Time Zone " + tzone);
		}

		this.lexer.match('\n');

		SIPDateHeader retval = new SIPDateHeader();

		retval.setDate(cal);

		logger.exiting(DateParser.class.getName(), "parse", retval);

		return retval;
	}
}
