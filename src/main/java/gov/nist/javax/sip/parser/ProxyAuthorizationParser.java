package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Parser for ProxyAuthorization headers.
 */
public class ProxyAuthorizationParser extends ChallengeParser {
	/**
	 * Constructor
	 * 
	 * @param proxyAuthorization -- header to parse
	 */
	public ProxyAuthorizationParser(String proxyAuthorization) {
		super(proxyAuthorization);
	}

	/**
	 * Constructor
	 * 
	 * @param Lexer lexer to set
	 */
	protected ProxyAuthorizationParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (ProxyAuthenticate object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		headerName(TokenTypes.PROXY_AUTHORIZATION);
		ProxyAuthorization proxyAuth = new ProxyAuthorization();
		super.parse(proxyAuth);
		return proxyAuth;
	}
}
