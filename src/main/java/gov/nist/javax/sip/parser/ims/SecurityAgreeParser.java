package gov.nist.javax.sip.parser.ims;

/**
 * Security Agreement for SIP.
 * <p>headers: Security-Client, Security-Server and Security-Verify</p>
 */
import gov.nist.core.NameValue;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeaderList;
import gov.nist.javax.sip.header.ims.*;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

public class SecurityAgreeParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SecurityAgreeParser.class.getName());

	public SecurityAgreeParser(String security) {
		super(security);
	}

	protected SecurityAgreeParser(Lexer lexer) {
		super(lexer);
	}

	protected void parseParameter(SecurityAgree header) throws ParseException {
		logger.entering(SecurityAgreeParser.class.getName(), "parseParameter");

		NameValue nv = this.nameValue('=');

		header.setParameter(nv);

		logger.exiting(SecurityAgreeParser.class.getName(), "parseParameter");
	}

	public SIPHeaderList parse(SecurityAgree header) throws ParseException {
		SIPHeaderList list;

		if (header.getClass().isInstance(new SecurityClient())) {
			list = new SecurityClientList();
		} else if (header.getClass().isInstance(new SecurityServer())) {
			list = new SecurityServerList();
		} else if (header.getClass().isInstance(new SecurityVerify())) {
			list = new SecurityVerifyList();
		} else
			return null;

		// the security-mechanism:
		this.lexer.SPorHT();
		lexer.match(TokenTypes.ID);
		Token type = lexer.getNextToken();
		header.setSecurityMechanism(type.getTokenValue());
		this.lexer.SPorHT();

		char la = lexer.lookAhead(0);
		if (la == '\n') {
			list.add(header);
			return list;
		} else if (la == ';')
			this.lexer.match(';');

		this.lexer.SPorHT();

		// The parameters:
		while (lexer.lookAhead(0) != '\n') {

			this.parseParameter(header);
			this.lexer.SPorHT();
			char laInLoop = lexer.lookAhead(0);
			if (laInLoop == '\n' || laInLoop == '\0') {
				break;
			}

			if(laInLoop == ',') {
				list.add(header);
				if (header.getClass().isInstance(new SecurityClient())) {
					header = new SecurityClient();
				} else if (header.getClass().isInstance(new SecurityServer())) {
					header = new SecurityServer();
				} else if (header.getClass().isInstance(new SecurityVerify())) {
					header = new SecurityVerify();
				}

				this.lexer.match(',');
				// the security-mechanism:
				this.lexer.SPorHT();
				lexer.match(TokenTypes.ID);
				type = lexer.getNextToken();
				header.setSecurityMechanism(type.getTokenValue());
			}

			this.lexer.SPorHT();

			if(lexer.lookAhead(0) == ';') {
				this.lexer.match(';');
			}

			this.lexer.SPorHT();
		}

		list.add(header);

		return list;
	}
}
