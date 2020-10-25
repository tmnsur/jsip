package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for RetryAfter header.
 */
public class RetryAfterParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(RetryAfterParser.class.getName());

	/**
	 * Creates a new instance of RetryAfterParser
	 * 
	 * @param retryAfter the header to parse
	 */
	public RetryAfterParser(String retryAfter) {
		super(retryAfter);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected RetryAfterParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (RetryAfter object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(RetryAfterParser.class.getName(), "parse");

		RetryAfter retryAfter = new RetryAfter();

		headerName(TokenTypes.RETRY_AFTER);

		// mandatory delay seconds:
		String value = lexer.number();

		try {
			retryAfter.setRetryAfter(Integer.parseInt(value));
		} catch (NumberFormatException | InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}

		this.lexer.SPorHT();

		if(lexer.lookAhead(0) == '(') {
			String comment = this.lexer.comment();
			retryAfter.setComment(comment);
		}

		this.lexer.SPorHT();

		while(lexer.lookAhead(0) == ';') {
			this.lexer.match(';');
			this.lexer.SPorHT();

			lexer.match(TokenTypes.ID);

			Token token = lexer.getNextToken();

			value = token.getTokenValue();

			if(value.equalsIgnoreCase("duration")) {
				this.lexer.match('=');
				this.lexer.SPorHT();

				value = lexer.number();

				try {
					retryAfter.setDuration(Integer.parseInt(value));
				} catch (NumberFormatException | InvalidArgumentException ex) {
					throw createParseException(ex.getMessage());
				}
			} else {
				this.lexer.SPorHT();
				this.lexer.match('=');
				this.lexer.SPorHT();

				lexer.match(TokenTypes.ID);

				Token secondToken = lexer.getNextToken();
				String secondValue = secondToken.getTokenValue();

				retryAfter.setParameter(value, secondValue);
			}

			this.lexer.SPorHT();
		}

		logger.exiting(RetryAfterParser.class.getName(), "parse", retryAfter);

		return retryAfter;
	}
}
