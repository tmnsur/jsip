package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for AllowEvents header.
 */
public class AllowEventsParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(AllowEventsParser.class.getName());

	/**
	 * Creates a new instance of AllowEventsParser
	 * 
	 * @param allowEvents the header to parse
	 */
	public AllowEventsParser(String allowEvents) {
		super(allowEvents);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected AllowEventsParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the AllowEvents String header
	 * 
	 * @return SIPHeader (AllowEventsList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AllowEventsParser.class.getName(), "parse");

		AllowEventsList list = new AllowEventsList();

		headerName(TokenTypes.ALLOW_EVENTS);

		AllowEvents allowEvents = new AllowEvents();

		this.lexer.match(TokenTypes.ID);

		Token token = lexer.getNextToken();

		allowEvents.setEventType(token.getTokenValue());

		list.add(allowEvents);

		this.lexer.SPorHT();

		while(lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			allowEvents = new AllowEvents();

			this.lexer.match(TokenTypes.ID);

			token = lexer.getNextToken();
			allowEvents.setEventType(token.getTokenValue());

			list.add(allowEvents);

			this.lexer.SPorHT();
		}

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(AllowEventsParser.class.getName(), "parse", list);

		return list;
	}
}
