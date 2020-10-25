package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Allow header.
 */
public class AllowParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(AllowParser.class.getName());

	/**
	 * Creates a new instance of AllowParser
	 * 
	 * @param allow the header to parse
	 */
	public AllowParser(String allow) {
		super(allow);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected AllowParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the Allow String header
	 * 
	 * @return SIPHeader (AllowList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AllowParser.class.getName(), "parse");

		AllowList list = new AllowList();

		headerName(TokenTypes.ALLOW);

		Allow allow = new Allow();
		list.add(allow);
		if (lexer.startsId()) {
			Token token = this.lexer.match(TokenTypes.ID);
			allow.setMethod(token.getTokenValue());
			this.lexer.SPorHT();
			while (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				allow = new Allow();
				token = this.lexer.match(TokenTypes.ID);
				allow.setMethod(token.getTokenValue());
				list.add(allow);
				this.lexer.SPorHT();
			}
		}

		this.lexer.match('\n');

		logger.exiting(AllowParser.class.getName(), "parse", list);

		return list;
	}
}
