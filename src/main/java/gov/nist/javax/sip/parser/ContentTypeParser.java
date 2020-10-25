package gov.nist.javax.sip.parser;

import gov.nist.core.*;
import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for content type header.
 */
public class ContentTypeParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ContentTypeParser.class.getName());

	public ContentTypeParser(String contentType) {
		super(contentType);
	}

	protected ContentTypeParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ContentTypeParser.class.getName(), "parse");

		ContentType contentType = new ContentType();

		this.headerName(TokenTypes.CONTENT_TYPE);

		// The type:
		lexer.match(TokenTypes.ID);

		Token type = lexer.getNextToken();

		this.lexer.SPorHT();

		contentType.setContentType(type.getTokenValue());

		// The sub-type:
		lexer.match('/');
		lexer.match(TokenTypes.ID);

		Token subType = lexer.getNextToken();

		this.lexer.SPorHT();

		contentType.setContentSubType(subType.getTokenValue());

		super.parse(contentType);

		this.lexer.match('\n');

		logger.exiting(ContentTypeParser.class.getName(), "parse", contentType);

		return contentType;
	}
}
