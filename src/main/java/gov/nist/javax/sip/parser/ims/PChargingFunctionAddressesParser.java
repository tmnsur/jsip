package gov.nist.javax.sip.parser.ims;

import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddresses;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * P-Charging-Function-Addresses header parser.
 *
 * <p>
 * Sintax (RFC 3455):
 * </p>
 * 
 * <pre>
 * P-Charging-Addr    = "P-Charging-Function-Addresses" HCOLON
 *                      charge-addr-params
 *                      * (SEMI charge-addr-params)
 * charge-addr-params = ccf / ecf / generic-param
 * ccf                = "ccf" EQUAL gen-value
 * ecf                = "ecf" EQUAL gen-value
 * gen-value          = token / host / quoted-string
 * host               = hostname / IPv4address / IPv6reference
 * hostname           = *( domainlabel "." ) toplabel [ "." ]
 * domainlabel        = alphanum / alphanum *( alphanum / "-" ) alphanum
 * toplabel           = ALPHA / ALPHA *( alphanum / "-" ) alphanum
 * ipv6reference      = "[" IPv6address "]"
 *
 * </pre>
 *
 * @author ALEXANDRE MIGUEL SILVA SANTOS
 * @author aayush.bhatnagar: proposed change to allow duplicate ecf and ccf
 *         header parameters.
 */
public class PChargingFunctionAddressesParser extends ParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PChargingFunctionAddressesParser.class.getName());

	public PChargingFunctionAddressesParser(String charging) {
		super(charging);
	}

	protected PChargingFunctionAddressesParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PChargingFunctionAddressesParser.class.getName(), "parse");

		headerName(TokenTypes.P_CHARGING_FUNCTION_ADDRESSES);
		PChargingFunctionAddresses chargingFunctionAddresses = new PChargingFunctionAddresses();

		while (lexer.lookAhead(0) != '\n') {

			this.parseParameter(chargingFunctionAddresses);
			this.lexer.SPorHT();
			char la = lexer.lookAhead(0);
			if (la == '\n' || la == '\0')
				break;

			this.lexer.match(';');
			this.lexer.SPorHT();
		}

		super.parse(chargingFunctionAddresses);

		logger.exiting(PChargingFunctionAddressesParser.class.getName(), "parse", chargingFunctionAddresses);

		return chargingFunctionAddresses;
	}

	protected void parseParameter(PChargingFunctionAddresses chargingFunctionAddresses) throws ParseException {
		logger.entering(PChargingFunctionAddressesParser.class.getName(), "parseParameter");

		NameValue nv = this.nameValue('=');

		chargingFunctionAddresses.setMultiParameter(nv);

		logger.exiting(PChargingFunctionAddressesParser.class.getName(), "parseParameter");
	}
}
