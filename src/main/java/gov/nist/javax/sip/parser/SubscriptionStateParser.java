package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for SubscriptionState header.
 */
public class SubscriptionStateParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SubscriptionStateParser.class.getName());

	/**
	 * Creates a new instance of SubscriptionStateParser
	 * 
	 * @param subscriptionState the header to parse
	 */
	public SubscriptionStateParser(String subscriptionState) {
		super(subscriptionState);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected SubscriptionStateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (SubscriptionState object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SubscriptionStateParser.class.getName(), "parse");

		SubscriptionState subscriptionState = new SubscriptionState();

		headerName(TokenTypes.SUBSCRIPTION_STATE);

		subscriptionState.setHeaderName(SIPHeaderNames.SUBSCRIPTION_STATE);

		// State:
		lexer.match(TokenTypes.ID);
		Token token = lexer.getNextToken();
		subscriptionState.setState(token.getTokenValue());

		while (lexer.lookAhead(0) == ';') {
			this.lexer.match(';');
			this.lexer.SPorHT();
			lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			String value = token.getTokenValue();
			if (value.equalsIgnoreCase("reason")) {
				this.lexer.match('=');
				this.lexer.SPorHT();
				lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				value = token.getTokenValue();
				subscriptionState.setReasonCode(value);
			} else if (value.equalsIgnoreCase("expires")) {
				this.lexer.match('=');
				this.lexer.SPorHT();

				lexer.match(TokenTypes.ID);

				token = lexer.getNextToken();
				value = token.getTokenValue();

				try {
					subscriptionState.setExpires(Integer.parseInt(value));
				} catch (NumberFormatException | InvalidArgumentException ex) {
					throw createParseException(ex.getMessage());
				}
			} else if(value.equalsIgnoreCase("retry-after")) {
				this.lexer.match('=');
				this.lexer.SPorHT();

				lexer.match(TokenTypes.ID);

				token = lexer.getNextToken();
				value = token.getTokenValue();

				try {
					subscriptionState.setRetryAfter(Integer.parseInt(value));
				} catch(NumberFormatException | InvalidArgumentException ex) {
					throw createParseException(ex.getMessage());
				}
			} else {
				this.lexer.match('=');
				this.lexer.SPorHT();

				lexer.match(TokenTypes.ID);

				Token secondToken = lexer.getNextToken();
				String secondValue = secondToken.getTokenValue();

				subscriptionState.setParameter(value, secondValue);
			}

			this.lexer.SPorHT();
		}

		logger.exiting(SubscriptionStateParser.class.getName(), "parse", subscriptionState);

		return subscriptionState;
	}
}
