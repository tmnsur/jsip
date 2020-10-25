package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Server;

/**
 * Parser for Server header.
 */
public class ServerParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ServerParser.class.getName());

	/**
	 * Creates a new instance of ServerParser
	 * 
	 * @param server the header to parse
	 */
	public ServerParser(String server) {
		super(server);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ServerParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String server
	 * 
	 * @return SIPHeader (Server object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ServerParser.class.getName(), "parse");

		Server server = new Server();

		headerName(TokenTypes.SERVER);

		if(this.lexer.lookAhead(0) == '\n') {
			throw createParseException("empty header");
		}

		// mandatory token: product[/product-version] | (comment)
		while(this.lexer.lookAhead(0) != '\n' && this.lexer.lookAhead(0) != '\0') {
			if(this.lexer.lookAhead(0) == '(') {
				String comment = this.lexer.comment();
				server.addProductToken('(' + comment + ')');
			} else {
				String tok;
				int marker = 0;

				try {
					marker = this.lexer.markInputPosition();
					tok = this.lexer.getString('/');

					if(tok.charAt(tok.length() - 1) == '\n') {
						tok = tok.trim();
					}

					server.addProductToken(tok);
				} catch(ParseException ex) {
					this.lexer.rewindInputPosition(marker);
					tok = this.lexer.getRest().trim();
					server.addProductToken(tok);
					break;
				}
			}
		}

		logger.exiting(ServerParser.class.getName(), "parse", server);

		return server;
	}
}
