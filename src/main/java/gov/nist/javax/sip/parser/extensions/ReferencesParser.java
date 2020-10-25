package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.References;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

public class ReferencesParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ReferencesParser.class.getName());

	public ReferencesParser(String references) {
		super(references);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ReferencesParser(Lexer lexer) {
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
		logger.entering(ReferencesParser.class.getName(), "parse");

		References references = null;
		try {
			headerName(TokenTypes.REFERENCES);

			references = new References();

			this.lexer.SPorHT();

			String callId = lexer.byteStringNoSemicolon();

			references.setCallId(callId);

			super.parse(references);

			return references;
		} finally {
			logger.exiting(ReferencesParser.class.getName(), "parse", references);
		}
	}
}
