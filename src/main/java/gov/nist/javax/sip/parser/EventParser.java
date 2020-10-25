package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Event header.
 */
public class EventParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(EventParser.class.getName());

	/**
	 * Creates a new instance of EventParser
	 * 
	 * @param event the header to parse
	 */
	public EventParser(String event) {
		super(event);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected EventParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (Event object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(EventParser.class.getName(), "parse");

		try {
			headerName(TokenTypes.EVENT);
			this.lexer.SPorHT();

			Event event = new Event();
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			String value = token.getTokenValue();

			event.setEventType(value);
			super.parse(event);

			this.lexer.SPorHT();
			this.lexer.match('\n');

			logger.exiting(EventParser.class.getName(), "parse", event);

			return event;
		} catch (ParseException ex) {
			throw createParseException(ex.getMessage());
		}
	}
}
