package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for ContentLanguage header.
 */
public class ContentDispositionParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ContentDispositionParser.class.getName());

	/**
	 * Creates a new instance of ContentDispositionParser
	 * 
	 * @param contentDisposition the header to parse
	 */
	public ContentDispositionParser(String contentDisposition) {
		super(contentDisposition);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ContentDispositionParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the ContentDispositionHeader String header
	 * 
	 * @return SIPHeader (ContentDispositionList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ContentDispositionParser.class.getName(), "parse");

		try {
			headerName(TokenTypes.CONTENT_DISPOSITION);

			ContentDisposition cd = new ContentDisposition();
			cd.setHeaderName(SIPHeaderNames.CONTENT_DISPOSITION);

			this.lexer.SPorHT();
			this.lexer.match(TokenTypes.ID);

			Token token = lexer.getNextToken();
			cd.setDispositionType(token.getTokenValue());
			this.lexer.SPorHT();
			super.parse(cd);

			this.lexer.SPorHT();
			this.lexer.match('\n');

			logger.exiting(ContentDispositionParser.class.getName(), "parse", cd);

			return cd;
		} catch(ParseException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
