package gov.nist.javax.sip.parser.ims;

/**
 * Security-Client header parser.
 */
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.SecurityClient;
import gov.nist.javax.sip.header.ims.SecurityClientList;

public class SecurityClientParser extends SecurityAgreeParser {
	private static final Logger logger = Logger.getLogger(SecurityClientParser.class.getName());

	public SecurityClientParser(String security) {
		super(security);
	}

	protected SecurityClientParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SecurityClientParser.class.getName(), "parse");

		headerName(TokenTypes.SECURITY_CLIENT);

		SecurityClient secClient = new SecurityClient();
		SecurityClientList secClientList = (SecurityClientList) super.parse(secClient);

		logger.exiting(SecurityClientParser.class.getName(), "parse", secClientList);

		return secClientList;
	}
}
