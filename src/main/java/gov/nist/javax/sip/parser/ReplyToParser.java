package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.*;

/**
 * Parser for a list of RelpyTo headers.
 */
public class ReplyToParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(ReplyToParser.class.getName());

	/**
	 * Creates a new instance of ReplyToParser
	 * 
	 * @param replyTo the header to parse
	 */
	public ReplyToParser(String replyTo) {
		super(replyTo);
	}

	/**
	 * Constructor param lexer the lexer to use to parse the header
	 */
	protected ReplyToParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message and generate the ReplyTo List Object
	 * 
	 * @return SIPHeader the ReplyTo List object
	 * @throws SIPParseException if errors occur during the parsing
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ReplyToParser.class.getName(), "parse");

		ReplyTo replyTo = new ReplyTo();

		headerName(TokenTypes.REPLY_TO);

		replyTo.setHeaderName(SIPHeaderNames.REPLY_TO);

		super.parse(replyTo);

		logger.exiting(ReplyToParser.class.getName(), "parse", replyTo);

		return replyTo;
	}
}
