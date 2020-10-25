package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Reason header.
 */
public class ReasonParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ReasonParser.class.getName());

	/**
	 * Creates a new instance of ReasonParser
	 * 
	 * @param reason the header to parse
	 */
	public ReasonParser(String reason) {
		super(reason);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ReasonParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (ReasonParserList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ReasonParser.class.getName(), "parse");

		ReasonList reasonList = new ReasonList();

		headerName(TokenTypes.REASON);
		this.lexer.SPorHT();
		while (lexer.lookAhead(0) != '\n') {
			Reason reason = new Reason();
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			String value = token.getTokenValue();

			reason.setProtocol(value);
			super.parse(reason);
			reasonList.add(reason);
			if (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else {
				this.lexer.SPorHT();
			}
		}

		logger.exiting(ReasonParser.class.getName(), "parse", reasonList);

		return reasonList;
	}
}
