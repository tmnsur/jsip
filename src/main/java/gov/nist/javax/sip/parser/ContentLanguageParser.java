package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for ContentLanguage header.
 */
public class ContentLanguageParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ContentLanguageParser.class.getName());

	/**
	 * Creates a new instance of ContentLanguageParser
	 * 
	 * @param contentLanguage the header to parse
	 */
	public ContentLanguageParser(String contentLanguage) {
		super(contentLanguage);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ContentLanguageParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the ContentLanguageHeader String header
	 * 
	 * @return SIPHeader (ContentLanguageList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ContentLanguageParser.class.getName(), "parse");

		ContentLanguageList list = new ContentLanguageList();

		try {
			headerName(TokenTypes.CONTENT_LANGUAGE);

			while (lexer.lookAhead(0) != '\n') {
				this.lexer.SPorHT();
				this.lexer.match(TokenTypes.ID);

				Token token = lexer.getNextToken();
				ContentLanguage cl = new ContentLanguage(token.getTokenValue());
				this.lexer.SPorHT();
				list.add(cl);

				while (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();
					this.lexer.match(TokenTypes.ID);
					this.lexer.SPorHT();
					token = lexer.getNextToken();
					cl = new ContentLanguage(token.getTokenValue());
					this.lexer.SPorHT();
					list.add(cl);
				}
			}

			logger.exiting(ContentLanguageParser.class.getName(), "parse", list);

			return list;
		} catch (ParseException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
