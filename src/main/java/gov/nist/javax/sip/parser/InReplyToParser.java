package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for InReplyTo header.
 */
public class InReplyToParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(InReplyToParser.class.getName());

	/**
	 * Creates a new instance of InReplyToParser
	 * 
	 * @param inReplyTo the header to parse
	 */
	public InReplyToParser(String inReplyTo) {
		super(inReplyTo);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected InReplyToParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (InReplyToList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(InReplyToParser.class.getName(), "parse");

		InReplyToList list = new InReplyToList();

		headerName(TokenTypes.IN_REPLY_TO);

		while (lexer.lookAhead(0) != '\n') {
			InReplyTo inReplyTo = new InReplyTo();
			inReplyTo.setHeaderName(SIPHeaderNames.IN_REPLY_TO);

			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			if (lexer.lookAhead(0) == '@') {
				this.lexer.match('@');
				this.lexer.match(TokenTypes.SAFE);
				Token secToken = lexer.getNextToken();
				inReplyTo.setCallId(token.getTokenValue() + "@" + secToken.getTokenValue());
			} else {
				inReplyTo.setCallId(token.getTokenValue());
			}

			this.lexer.SPorHT();

			list.add(inReplyTo);

			while(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				inReplyTo = new InReplyTo();

				this.lexer.match(TokenTypes.ID);

				token = lexer.getNextToken();

				if(lexer.lookAhead(0) == '@') {
					this.lexer.match('@');
					this.lexer.match(TokenTypes.SAFE);

					Token secToken = lexer.getNextToken();

					inReplyTo.setCallId(token.getTokenValue() + "@" + secToken.getTokenValue());
				} else {
					inReplyTo.setCallId(token.getTokenValue());
				}

				list.add(inReplyTo);
			}
		}

		logger.exiting(InReplyToParser.class.getName(), "parse", list);

		return list;
	}
}
