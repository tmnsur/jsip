package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for RSeq header.
 */
public class RSeqParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(RSeqParser.class.getName());

	/**
	 * Creates a new instance of RSeqParser
	 * 
	 * @param rseq the header to parse
	 */
	public RSeqParser(String rseq) {
		super(rseq);
	}

	/**
	 * Constructor param lexer the lexer to use to parse the header
	 */
	protected RSeqParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader ( RSeq object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(RSeqParser.class.getName(), "parse");

		RSeq rseq = new RSeq();
		headerName(TokenTypes.RSEQ);

		rseq.setHeaderName(SIPHeaderNames.RSEQ);

		String number = this.lexer.number();
		try {
			rseq.setSeqNumber(Long.parseLong(number));
		} catch (InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(RSeqParser.class.getName(), "parse", rseq);

		return rseq;
	}
}
