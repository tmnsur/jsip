package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Accept header.
 *
 * @version 1.2 $Revision: 1.7 $ $Date: 2009-07-17 18:57:56 $
 */
public class AcceptParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(AcceptParser.class.getName());

	/**
	 * Creates a new instance of Accept Parser
	 * 
	 * @param accept the header to parse
	 */
	public AcceptParser(String accept) {
		super(accept);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected AcceptParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the Accept String header
	 * 
	 * @return SIPHeader (AcceptList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		logger.entering(AcceptParser.class.getName(), "parse");

		AcceptList list = new AcceptList();

		headerName(TokenTypes.ACCEPT);

		Accept accept = new Accept();
		accept.setHeaderName(SIPHeaderNames.ACCEPT);

		this.lexer.SPorHT();
		if (lexer.startsId()) { // allow can be empty
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			accept.setContentType(token.getTokenValue());
			this.lexer.match('/');
			this.lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			accept.setContentSubType(token.getTokenValue());
			this.lexer.SPorHT();
			super.parse(accept);
		}
		list.add(accept);

		while (lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			accept = new Accept();
			if (lexer.startsId()) {
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				accept.setContentType(token.getTokenValue());
				this.lexer.match('/');
				this.lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				accept.setContentSubType(token.getTokenValue());
				this.lexer.SPorHT();
				super.parse(accept);
			}
			list.add(accept);
		}

		logger.exiting(AcceptParser.class.getName(), "parse", list);

		return list;
	}
}
