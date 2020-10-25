package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.parser.*;

import java.text.ParseException;
import java.util.logging.Logger;

// Parser for Replaces Header (RFC3891)
// Extension by pmusgrave
//
// Replaces        = "Replaces" HCOLON callid *(SEMI replaces-param)
// replaces-param  = to-tag / from-tag / early-flag / generic-param
// to-tag          = "to-tag" EQUAL token
// from-tag        = "from-tag" EQUAL token
// early-flag      = "early-only"
//
//
public class ReplacesParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ReplacesParser.class.getName());

	/**
	 * Creates new CallIDParser
	 * 
	 * @param callID message to parse
	 */
	public ReplacesParser(String callID) {
		super(callID);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected ReplacesParser(Lexer lexer) {
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
		logger.entering(ReplacesParser.class.getName(), "parse");

		Replaces replaces = null;
		try {
			headerName(TokenTypes.REPLACES_TO);

			replaces = new Replaces();

			this.lexer.SPorHT();

			String callId = lexer.byteStringNoSemicolon();

			this.lexer.SPorHT();

			super.parse(replaces);

			replaces.setCallId(callId);

			return replaces;
		} finally {
			logger.exiting(ReplacesParser.class.getName(), "parse", replaces);
		}
	}
}
