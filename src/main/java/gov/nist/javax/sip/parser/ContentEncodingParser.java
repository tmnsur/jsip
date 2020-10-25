package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for ContentLanguage header.
 */
public class ContentEncodingParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ContentEncodingParser.class.getName());

	/**
	 * Creates a new instance of ContentEncodingParser
	 * 
	 * @param contentEncoding the header to parse
	 */
	public ContentEncodingParser(String contentEncoding) {
		super(contentEncoding);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ContentEncodingParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the ContentEncodingHeader String header
	 * 
	 * @return SIPHeader (ContentEncodingList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ContentEncodingParser.class.getName(), "parse");

		ContentEncodingList list = new ContentEncodingList();

		try {
			headerName(TokenTypes.CONTENT_ENCODING);

			while (lexer.lookAhead(0) != '\n') {
				ContentEncoding cl = new ContentEncoding();
				cl.setHeaderName(SIPHeaderNames.CONTENT_ENCODING);

				this.lexer.SPorHT();
				this.lexer.match(TokenTypes.ID);

				Token token = lexer.getNextToken();
				cl.setEncoding(token.getTokenValue());

				this.lexer.SPorHT();
				list.add(cl);

				while (lexer.lookAhead(0) == ',') {
					cl = new ContentEncoding();
					this.lexer.match(',');
					this.lexer.SPorHT();
					this.lexer.match(TokenTypes.ID);
					this.lexer.SPorHT();
					token = lexer.getNextToken();
					cl.setEncoding(token.getTokenValue());
					this.lexer.SPorHT();
					list.add(cl);
				}
			}

			logger.exiting(ContentEncodingParser.class.getName(), "parse", list);

			return list;
		} catch (ParseException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
