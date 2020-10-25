package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.RecordRoute;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.SIPHeader;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for a list of route headers.
 */
public class RecordRouteParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(RecordRouteParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param recordRoute message to parse to set
	 */
	public RecordRouteParser(String recordRoute) {
		super(recordRoute);
	}

	protected RecordRouteParser(Lexer lexer) {
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
		logger.entering(RecordRouteParser.class.getName(), "parse");

		RecordRouteList recordRouteList = new RecordRouteList();

		this.lexer.match(TokenTypes.RECORD_ROUTE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
		while (true) {
			RecordRoute recordRoute = new RecordRoute();
			super.parse(recordRoute);
			recordRouteList.add(recordRoute);
			this.lexer.SPorHT();
			char la = lexer.lookAhead(0);
			if (la == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else if (la == '\n') {
				break;
			} else {
				throw createParseException("unexpected char");
			}
		}

		logger.exiting(RecordRouteParser.class.getName(), "parse", recordRouteList);

		return recordRouteList;
	}
}
