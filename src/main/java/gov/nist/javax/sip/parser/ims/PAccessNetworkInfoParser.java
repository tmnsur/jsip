package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.PAccessNetworkInfo;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;
import gov.nist.core.Token;
import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * P-Access-Network-Info header parser.
 *
 * <p>
 * RFC 3455 - Private Header (P-Header) Extensions to the Session Initiation
 * Protocol (SIP) for the 3rd-Generation Partnership Project (3GPP)
 * </p>
 *
 * <p>
 * Syntax (RFC 3455):
 * </p>
 * 
 * <pre>
 * P-Access-Network-Info  = "P-Access-Network-Info" HCOLON access-net-spec
 * access-net-spec        = access-type *(SEMI access-info)
 * access-type            = "IEEE-802.11a" / "IEEE-802.11b" /
 *                          "3GPP-GERAN" / "3GPP-UTRAN-FDD" /
 *                          "3GPP-UTRAN-TDD" / "3GPP-CDMA2000" / token
 * access-info            = cgi-3gpp / utran-cell-id-3gpp / extension-access-info
 * extension-access-info  = gen-value
 * cgi-3gpp               = "cgi-3gpp" EQUAL (token / quoted-string)
 * utran-cell-id-3gpp     = "utran-cell-id-3gpp" EQUAL (token / quoted-string)
 * gen-value              = token / host / quoted-string
 * </pre>
 * 
 * <p>
 * RFC 7913 - P-Access-Network-Info ABNF Update
 * </p>
 * <p>
 * Newer RFC https://tools.ietf.org/html/rfc7913
 * </p>
 * 
 * <pre>
 *       access-info            = cgi-3gpp / utran-cell-id-3gpp /
 *                                dsl-location / i-wlan-node-id /
 *                                ci-3gpp2 / eth-location /
 *                                ci-3gpp2-femto / fiber-location /
 *                                np / gstn-location /local-time-zone /
 *                                dvb-rcs2-node-id / operator-specific-GI /
 *                                utran-sai-3gpp / extension-access-info
 *       np                     = "network-provided"
 *       extension-access-info  = generic-param
 * </pre>
 */
public class PAccessNetworkInfoParser extends HeaderParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PAccessNetworkInfoParser.class.getName());

	public PAccessNetworkInfoParser(String accessNetwork) {
		super(accessNetwork);
	}

	protected PAccessNetworkInfoParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PAccessNetworkInfoParser.class.getName(), "parse");

		headerName(TokenTypes.P_ACCESS_NETWORK_INFO);
		PAccessNetworkInfo accessNetworkInfo = new PAccessNetworkInfo();
		accessNetworkInfo.setHeaderName(SIPHeaderNamesIms.P_ACCESS_NETWORK_INFO);

		this.lexer.SPorHT();
		lexer.match(TokenTypes.ID);
		Token token = lexer.getNextToken();
		accessNetworkInfo.setAccessType(token.getTokenValue());

		this.lexer.SPorHT();
		while (lexer.lookAhead(0) == ';') {
			this.lexer.match(';');
			this.lexer.SPorHT();

			try {
				NameValue nv = super.nameValue('=');
				accessNetworkInfo.setParameter(nv);
			} catch (ParseException e) {
				this.lexer.SPorHT();
				String ext = this.lexer.quotedString();
				if (ext == null) {
					ext = this.lexer.ttokenGenValue();
				} else {
					// avoids tokens such as "a=b" to be stripped of quotes and misinterpretend as
					// RFC 7913 generic-param when re-encoded
					ext = "\"" + ext + "\"";
				}
				accessNetworkInfo.setExtensionAccessInfo(ext);
			}
			this.lexer.SPorHT();
		}
		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(PAccessNetworkInfoParser.class.getName(), "parse", accessNetworkInfo);

		return accessNetworkInfo;
	}
}
