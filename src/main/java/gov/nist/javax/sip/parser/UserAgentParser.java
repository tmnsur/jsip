package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.UserAgent;

/**
 * Parser for UserAgent header.
 */
public class UserAgentParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(UserAgentParser.class.getName());

	/**
	 * Constructor
	 *
	 * @param userAgent - UserAgent header to parse
	 */
	public UserAgentParser(String userAgent) {
		super(userAgent);
	}

	/**
	 * Constructor
	 *
	 * @param lexer - the lexer to use.
	 */
	protected UserAgentParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the message. Note that we have losened up on the parsing quite a bit
	 * because user agents tend to be very bad about specifying the user agent
	 * according to RFC.
	 *
	 * @return SIPHeader (UserAgent object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(UserAgentParser.class.getName(), "parse");

		UserAgent userAgent = new UserAgent();

		headerName(TokenTypes.USER_AGENT);

		if(this.lexer.lookAhead(0) == '\n') {
			throw createParseException("empty header");
		}

		/*
		 * BNF User-Agent = "User-Agent" HCOLON server-val *(LWS server-val) server-val
		 * = product / comment product = token [SLASH product-version] product-version =
		 * token
		 */
		while(this.lexer.lookAhead(0) != '\n' && this.lexer.lookAhead(0) != '\0') {
			if(this.lexer.lookAhead(0) == '(') {
				String comment = this.lexer.comment();

				userAgent.addProductToken('(' + comment + ')');
			} else {
				// product = token [SLASHproduct-version]
				// product-version = token
				// The RFC Does NOT allow this space but we are generous in what we accept

				this.getLexer().SPorHT();

				String product = this.lexer.byteStringNoSlash();
				if (product == null)
					throw createParseException("Expected product string");

				StringBuilder productSb = new StringBuilder(product);
				// do we possibly have the optional product-version?
				if (this.lexer.peekNextToken().getTokenType() == TokenTypes.SLASH) {
					// yes
					this.lexer.match(TokenTypes.SLASH);
					// product-version
					// The RFC Does NOT allow this space but we are generous in what we accept
					this.getLexer().SPorHT();

					String productVersion = this.lexer.byteStringNoSlash();

					if(productVersion == null) {
						throw createParseException("Expected product version");
					}

					productSb.append("/");

					productSb.append(productVersion);
				}

				userAgent.addProductToken(productSb.toString());
			}
			// LWS
			this.lexer.SPorHT();
		}

		logger.exiting(UserAgentParser.class.getName(), "parse", userAgent);

		return userAgent;
	}
}
