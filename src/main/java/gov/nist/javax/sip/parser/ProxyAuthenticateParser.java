package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Parser for ProxyAuthenticate headers.
 */
public class ProxyAuthenticateParser extends ChallengeParser {
	/**
	 * Constructor
	 * 
	 * @param proxyAuthenticate message to parse
	 */
	public ProxyAuthenticateParser(String proxyAuthenticate) {
		super(proxyAuthenticate);
	}

	/**
	 * Constructor
	 * 
	 * @param Lexer lexer to set
	 */
	protected ProxyAuthenticateParser(Lexer lexer) {
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
		headerName(TokenTypes.PROXY_AUTHENTICATE);

		ProxyAuthenticate proxyAuthenticate = new ProxyAuthenticate();

		super.parse(proxyAuthenticate);

		return proxyAuthenticate;
	}
}
