package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PPreferredService;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * Parse this: P-Preferred-Service:
 * urn:urn-7:3gpp-service.exampletelephony.version1
 */
public class PPreferredServiceParser extends HeaderParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PPreferredServiceParser.class.getName());

	protected PPreferredServiceParser(Lexer lexer) {
		super(lexer);
	}

	public PPreferredServiceParser(String pps) {
		super(pps);
	}

	/**
	 * "The URN consists of a hierarchical service identifier or application
	 * identifier, with a sequence of labels separated by periods.The left-most
	 * label is the most significant one and is called 'top-level service
	 * identifier', while names to the right are called 'sub-services' or
	 * 'sub-applications'.
	 *
	 * For any given service identifier, labels can be removed right-to-left and the
	 * resulting URN is still valid, referring a more generic service, with the
	 * exception of the top-level service identifier and possibly the first
	 * sub-service or sub-application identifier.
	 *
	 * Labels cannot be removed beyond a defined basic service, for example, the
	 * label w.x may define a service, but the label w may only define an assignment
	 * authority for assigning subsequent values and not define a service in its own
	 * right. In other words, if a service identifier 'w.x.y.z' exists, the URNs
	 * 'w.x' and 'w.x.y' are also valid service identifiers, but w may not be a
	 * valid service identifier if it merely defines who is responsible"
	 *
	 * 
	 *
	 *         Sub-service and Application identifiers are not maintained by IANA
	 *         and are organization/application dependent (Section 8.2). So we
	 *         cannot gurantee what lies beyond the first sub-service or
	 *         sub-application identifier. It should be the responsibility of the
	 *         application to make sense of the entire URN holistically. We can only
	 *         check for the standardized part as per the ABNF.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PPreferredServiceParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.P_PREFERRED_SERVICE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		PPreferredService pps = new PPreferredService();
		String urn = this.lexer.getBuffer();
		if (urn.contains(ParameterNamesIms.SERVICE_ID)) {

			if (urn.contains(ParameterNamesIms.SERVICE_ID_LABEL)) {
				String serviceID = urn.split(ParameterNamesIms.SERVICE_ID_LABEL + ".")[1];

				if (serviceID.trim().equals(""))
					try {
						throw new InvalidArgumentException("URN should atleast have one sub-service");
					} catch (InvalidArgumentException e) {

						e.printStackTrace();
					}
				else
					pps.setSubserviceIdentifiers(serviceID);
			} else if (urn.contains(ParameterNamesIms.APPLICATION_ID_LABEL)) {
				String appID = urn.split(ParameterNamesIms.APPLICATION_ID_LABEL)[1];
				if (appID.trim().equals(""))
					try {
						throw new InvalidArgumentException("URN should atleast have one sub-application");
					} catch (InvalidArgumentException e) {
						e.printStackTrace();
					}
				else
					pps.setApplicationIdentifiers(appID);
			} else {
				try {
					throw new InvalidArgumentException("URN is not well formed");

				} catch (InvalidArgumentException e) {
					e.printStackTrace();
				}
			}
		}

		super.parse();

		logger.exiting(PPreferredServiceParser.class.getName(), "parse", pps);

		return pps;
	}
}
