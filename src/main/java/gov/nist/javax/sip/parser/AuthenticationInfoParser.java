package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Authentication-Info header.
 */
public class AuthenticationInfoParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(AuthenticationInfoParser.class.getName());

	/**
	 * Creates a new instance of AuthenticationInfoParser
	 * 
	 * @param authenticationInfo the header to parse
	 */
	public AuthenticationInfoParser(String authenticationInfo) {
		super(authenticationInfo);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected AuthenticationInfoParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the AuthenticationInfo String header
	 * 
	 * @return SIPHeader (AuthenticationInfoList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AuthenticationInfoParser.class.getName(), "parse");

		headerName(TokenTypes.AUTHENTICATION_INFO);

		AuthenticationInfo authenticationInfo = new AuthenticationInfo();

		authenticationInfo.setHeaderName(SIPHeaderNames.AUTHENTICATION_INFO);

		this.lexer.SPorHT();

		NameValue nv = super.nameValue();

		if("".equals(nv.getValue()) && !nv.isValueQuoted()) {
			authenticationInfo.setScheme(nv.getKey());
			nv = super.nameValue();
		}

		authenticationInfo.setParameter(nv);

		this.lexer.SPorHT();

		while(lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			nv = super.nameValue();
			authenticationInfo.setParameter(nv);

			this.lexer.SPorHT();
		}

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(AuthenticationInfoParser.class.getName(), "parse", authenticationInfo);

		return authenticationInfo;
	}
}
