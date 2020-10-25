package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.PCalledPartyID;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import gov.nist.javax.sip.parser.AddressParametersParser;

/**
 * P-Called-Party-ID header parser
 */
public class PCalledPartyIDParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(PCalledPartyIDParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param calledPartyID content to set
	 */
	public PCalledPartyIDParser(String calledPartyID) {
		super(calledPartyID);
	}

	protected PCalledPartyIDParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PCalledPartyIDParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.P_CALLED_PARTY_ID);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		PCalledPartyID calledPartyID = new PCalledPartyID();

		super.parse(calledPartyID);

		logger.exiting(PCalledPartyIDParser.class.getName(), "parse", calledPartyID);

		return calledPartyID;
	}
}
