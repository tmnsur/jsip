package gov.nist.javax.sip.parser.ims;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PVisitedNetworkID;
import gov.nist.javax.sip.header.ims.PVisitedNetworkIDList;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * P-Visited-Network-ID header parser.
 *
 * <pre>
 * P-Visited-Network-ID   = "P-Visited-Network-ID" HCOLON
 *                          vnetwork-spec
 *                          *(COMMA vnetwork-spec)
 * vnetwork-spec          = (token / quoted-string)
 *                          *(SEMI vnetwork-param)
 * vnetwork-param         = generic-param
 * </pre>
 */
public class PVisitedNetworkIDParser extends ParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PVisitedNetworkIDParser.class.getName());

	/**
	 * Constructor
	 */
	public PVisitedNetworkIDParser(String networkID) {
		super(networkID);
	}

	protected PVisitedNetworkIDParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PVisitedNetworkIDParser.class.getName(), "parse");

		PVisitedNetworkIDList visitedNetworkIDList = new PVisitedNetworkIDList();

		this.lexer.match(TokenTypes.P_VISITED_NETWORK_ID);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		while(true) {
			PVisitedNetworkID visitedNetworkID = new PVisitedNetworkID();

			if (this.lexer.lookAhead(0) == '\"')
				parseQuotedString(visitedNetworkID);
			else
				parseToken(visitedNetworkID);

			visitedNetworkIDList.add(visitedNetworkID);

			this.lexer.SPorHT();
			char la = lexer.lookAhead(0);

			if(la == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else if(la == '\n') {
				break;
			} else {
				throw createParseException("unexpected char = " + la);
			}
		}

		logger.exiting(PVisitedNetworkIDParser.class.getName(), "parse", visitedNetworkIDList);

		return visitedNetworkIDList;
	}

	protected void parseQuotedString(PVisitedNetworkID visitedNetworkID) throws ParseException {
		logger.entering(PVisitedNetworkIDParser.class.getName(), "parseQuotedString");

		StringBuilder retval = new StringBuilder();

		if (this.lexer.lookAhead(0) != '\"')
			throw createParseException("unexpected char");
		this.lexer.consume(1);

		while (true) {
			char next = this.lexer.getNextChar();
			if (next == '\"') {
				// Got to the terminating quote.
				break;
			} else if (next == '\0') {
				throw new ParseException("unexpected EOL", 1);
			} else if (next == '\\') {
				retval.append(next);
				next = this.lexer.getNextChar();
				retval.append(next);
			} else {
				retval.append(next);
			}
		}

		visitedNetworkID.setVisitedNetworkID(retval.toString());

		super.parse(visitedNetworkID);

		logger.exiting(PVisitedNetworkIDParser.class.getName(), "parseQuotedString");
	}

	protected void parseToken(PVisitedNetworkID visitedNetworkID) throws ParseException {
		lexer.match(TokenTypes.ID);

		Token token = lexer.getNextToken();

		visitedNetworkID.setVisitedNetworkID(token);

		super.parse(visitedNetworkID);
	}
}
