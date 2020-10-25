package gov.nist.javax.sip.parser;

import javax.sip.*;
import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Content-Length Header.
 */
public class ContentLengthParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ContentLengthParser.class.getName());

	public ContentLengthParser(String contentLength) {
		super(contentLength);
	}

	protected ContentLengthParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ContentLengthParser.class.getName(), "parse");

		try {
			ContentLength contentLength = new ContentLength();

			headerName(TokenTypes.CONTENT_LENGTH);

			String number = this.lexer.number();

			contentLength.setContentLength(Integer.parseInt(number));

			this.lexer.SPorHT();
			this.lexer.match('\n');

			logger.exiting(ContentLengthParser.class.getName(), "parse", contentLength);

			return contentLength;
		} catch (InvalidArgumentException | NumberFormatException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
