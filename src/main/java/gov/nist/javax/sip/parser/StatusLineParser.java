package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.StatusLine;

/**
 * Parser for the SIP status line.
 */
public class StatusLineParser extends Parser {
	private static final Logger logger = Logger.getLogger(StatusLineParser.class.getName());

	public StatusLineParser(String statusLine) {
		this.lexer = new Lexer("status_lineLexer", statusLine);
	}

	public StatusLineParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("status_lineLexer");
	}

	protected int statusCode() throws ParseException {
		logger.entering(StatusLineParser.class.getName(), "statusCode");

		String scode = this.lexer.number();

		try {
			int retval = Integer.parseInt(scode);

			logger.exiting(StatusLineParser.class.getName(), "statusCode", retval);

			return retval;
		} catch(NumberFormatException ex) {
			throw new ParseException(lexer.getBuffer() + ":" + ex.getMessage(), lexer.getPtr());
		}
	}

	protected String reasonPhrase() {
		return this.lexer.getRest().trim();
	}

	public StatusLine parse() throws ParseException {
		logger.entering(StatusLineParser.class.getName(), "parse");

		StatusLine retval = new StatusLine();
		String version = this.sipVersion();

		retval.setSipVersion(version);

		lexer.SPorHT();

		int scode = statusCode();

		retval.setStatusCode(scode);

		lexer.SPorHT();

		String rp = reasonPhrase();

		retval.setReasonPhrase(rp);

		lexer.SPorHT();

		logger.exiting(StatusLineParser.class.getName(), "parse", retval);

		return retval;
	}
}
