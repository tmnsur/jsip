package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;

/**
 * Parser for a list of route headers.
 */
public class RouteParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(RouteParser.class.getName());
	/**
	 * Constructor
	 * 
	 * @param route message to parse to set
	 */
	public RouteParser(String route) {
		super(route);
	}

	protected RouteParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message and generate the Route List Object
	 * 
	 * @return SIPHeader the Route List object
	 * @throws SIPParseException if errors occur during the parsing
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(RouteParser.class.getName(), "parse");

		RouteList routeList = new RouteList();

		this.lexer.match(TokenTypes.ROUTE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		while(true) {
			Route route = new Route();

			super.parse(route);
			routeList.add(route);
			this.lexer.SPorHT();

			char la = lexer.lookAhead(0);
			if(la == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else if(la == '\n') {
				break;
			} else {
				throw createParseException("unexpected char");
			}
		}

		logger.exiting(RouteParser.class.getName(), "parse", routeList);

		return routeList;
	}
}
