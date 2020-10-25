package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for RAck header.
 */
public class RAckParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(RAckParser.class.getName());

	/**
	 * Creates a new instance of RAckParser
	 * 
	 * @param rack the header to parse
	 */
	public RAckParser(String rack) {
		super(rack);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected RAckParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (RAck object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(RAckParser.class.getName(), "parse");

		RAck rack = new RAck();

		headerName(TokenTypes.RACK);

		rack.setHeaderName(SIPHeaderNames.RACK);

		try {
			String number = this.lexer.number();
			rack.setRSequenceNumber(Long.parseLong(number));
			this.lexer.SPorHT();
			number = this.lexer.number();
			rack.setCSequenceNumber(Long.parseLong(number));
			this.lexer.SPorHT();
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			rack.setMethod(token.getTokenValue());

		} catch (InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}
		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(RAckParser.class.getName(), "parse", rack);

		return rack;
	}
}
