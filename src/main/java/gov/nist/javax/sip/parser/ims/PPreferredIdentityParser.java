package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PPreferredIdentity;

import gov.nist.javax.sip.parser.AddressParametersParser;

/**
 * P-Preferred-Identity header parser.
 */
public class PPreferredIdentityParser extends AddressParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PPreferredIdentityParser.class.getName());

	public PPreferredIdentityParser(String preferredIdentity) {
		super(preferredIdentity);
	}

	protected PPreferredIdentityParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PPreferredIdentityParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.P_PREFERRED_IDENTITY);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		PPreferredIdentity p = new PPreferredIdentity();

		super.parse(p);

		logger.exiting(PPreferredIdentityParser.class.getName(), "parse", p);

		return p;
	}
}
