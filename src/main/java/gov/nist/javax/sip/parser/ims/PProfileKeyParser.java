package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PProfileKey;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

public class PProfileKeyParser extends AddressParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PProfileKeyParser.class.getName());

	protected PProfileKeyParser(Lexer lexer) {
		super(lexer);
	}

	public PProfileKeyParser(String profilekey) {
		super(profilekey);
	}

	public SIPHeader parse() throws ParseException {
		logger.entering(PProfileKeyParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.P_PROFILE_KEY);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		PProfileKey p = new PProfileKey();

		super.parse(p);

		logger.exiting(PProfileKeyParser.class.getName(), "parse", p);

		return p;
	}
}
