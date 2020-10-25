package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.SecurityServerList;
import gov.nist.javax.sip.header.ims.SecurityServer;

/**
 * Security-Server header parser.
 */
public class SecurityServerParser extends SecurityAgreeParser {
	private static final Logger logger = Logger.getLogger(SecurityServerParser.class.getName());

	public SecurityServerParser(String security) {
		super(security);
	}

	protected SecurityServerParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SecurityServerParser.class.getName(), "parse");

		headerName(TokenTypes.SECURITY_SERVER);
		SecurityServer secServer = new SecurityServer();
		SecurityServerList secServerList = (SecurityServerList) super.parse(secServer);

		logger.exiting(SecurityServerParser.class.getName(), "parse", secServerList);

		return secServerList;
	}
}
