package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.SecurityVerify;
import gov.nist.javax.sip.header.ims.SecurityVerifyList;

/**
 * Security-Verify header parser.
 */
public class SecurityVerifyParser extends SecurityAgreeParser {
	private static final Logger logger = Logger.getLogger(SecurityVerifyParser.class.getName());

	public SecurityVerifyParser(String security) {
		super(security);
	}

	protected SecurityVerifyParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SecurityVerifyParser.class.getName(), "parse");

		headerName(TokenTypes.SECURITY_VERIFY);

		SecurityVerify secVerify = new SecurityVerify();
		SecurityVerifyList secVerifyList = (SecurityVerifyList) super.parse(secVerify);

		logger.entering(SecurityVerifyParser.class.getName(), "parse");

		return secVerifyList;
	}
}
