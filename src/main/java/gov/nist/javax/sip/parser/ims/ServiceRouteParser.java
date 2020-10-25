package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.ServiceRoute;
import gov.nist.javax.sip.header.ims.ServiceRouteList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * Service-Route header parser.
 */
public class ServiceRouteParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(ServiceRouteParser.class.getName());

	/**
	 * Constructor
	 */
	public ServiceRouteParser(String serviceRoute) {
		super(serviceRoute);
	}

	protected ServiceRouteParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message and generate the RecordRoute List Object
	 * 
	 * @return SIPHeader the RecordRoute List object
	 * @throws ParseException if errors occur during the parsing
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ServiceRouteParser.class.getName(), "parse");

		ServiceRouteList serviceRouteList = new ServiceRouteList();

		this.lexer.match(TokenTypes.SERVICE_ROUTE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		while(true) {
			ServiceRoute serviceRoute = new ServiceRoute();

			super.parse(serviceRoute);

			serviceRouteList.add(serviceRoute);

			this.lexer.SPorHT();

			if(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else if(lexer.lookAhead(0) == '\n') {
				break;
			} else {
				throw createParseException("unexpected char");
			}
		}

		logger.exiting(ServiceRouteParser.class.getName(), "parse", serviceRouteList);

		return serviceRouteList;
	}
}
