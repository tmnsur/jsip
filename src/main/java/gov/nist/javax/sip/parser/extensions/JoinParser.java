package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.parser.*;

import java.text.ParseException;
import java.util.logging.Logger;

// Parser for Join Header (RFC3911)
// Extension by jean deruelle
//
// Join        = "Join" HCOLON callid *(SEMI join-param)
// join-param  = to-tag / from-tag / generic-param
// to-tag          = "to-tag" EQUAL token
// from-tag        = "from-tag" EQUAL token
//
//

public class JoinParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(JoinParser.class.getName());

	/**
	 * Creates new CallIDParser
	 * 
	 * @param callID message to parse
	 */
	public JoinParser(String callID) {
		super(callID);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected JoinParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (CallID object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(JoinParser.class.getName(), "parse");

		Join join = null;
		try {
			headerName(TokenTypes.JOIN_TO);
			join = new Join();

			this.lexer.SPorHT();
			String callId = lexer.byteStringNoSemicolon();
			this.lexer.SPorHT();
			super.parse(join);
			join.setCallId(callId);
			return join;
		} finally {
			logger.exiting(JoinParser.class.getName(), "parse", join);
		}
	}
}
