package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AcceptEncoding;
import gov.nist.javax.sip.header.AcceptEncodingList;
import gov.nist.javax.sip.header.SIPHeader;

/**
 * Accept-Encoding SIP (HTTP) Header parser.
 *
 * <pre>
 *
 *   The Accept-Encoding request-header field is similar to Accept, but
 *   restricts the content-codings (section 3.5) that are acceptable in
 *   the response.
 *
 *
 *       Accept-Encoding  = "Accept-Encoding" ":"
 *                      ( encoding *( "," encoding) )
 *       encoding         = ( codings *[ ";" "q" "=" qvalue ] )
 *       codings          = ( content-coding | "*" )
 *
 *   Examples of its use are:
 *
 *       Accept-Encoding: compress, gzip
 *       Accept-Encoding:
 *       Accept-Encoding: *
 *       Accept-Encoding: compress;q=0.5, gzip;q=1.0
 *       Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
 * </pre>
 *
 */
public class AcceptEncodingParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(AcceptEncodingParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param acceptEncoding message to parse
	 */
	public AcceptEncodingParser(String acceptEncoding) {
		super(acceptEncoding);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected AcceptEncodingParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (AcceptEncoding object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AcceptEncodingParser.class.getName(), "parse");

		AcceptEncodingList acceptEncodingList = new AcceptEncodingList();

		headerName(TokenTypes.ACCEPT_ENCODING);

		// empty body is fine for this header.
		if(!lexer.startsId()) {
			acceptEncodingList.add(new AcceptEncoding());
		} else {
			do {
				AcceptEncoding acceptEncoding = new AcceptEncoding();
				if(lexer.startsId()) {
					Token value = lexer.match(TokenTypes.ID);
					acceptEncoding.setEncoding(value.getTokenValue());

					while(lexer.lookAhead(0) == ';') {
						this.lexer.match(';');
						this.lexer.SPorHT();

						Token pname = lexer.match(TokenTypes.ID); // Also allows generic param!

						this.lexer.SPorHT();

						if(lexer.lookAhead() == '=') {
							this.lexer.match('=');
							this.lexer.SPorHT();

							value = lexer.match(TokenTypes.ID);

							if(pname.getTokenValue().equalsIgnoreCase("q")) {
								try {
									acceptEncoding.setQValue(Float.parseFloat(value.getTokenValue()));
								} catch(NumberFormatException | InvalidArgumentException ex) {
									throw createParseException(ex.getMessage());
								}
							} else {
								acceptEncoding.setParameter(pname.getTokenValue(), value.getTokenValue());
							}

							this.lexer.SPorHT();
						} else {
							acceptEncoding.setParameter(pname.getTokenValue(), "");
						}
					}
				}

				acceptEncodingList.add(acceptEncoding);

				if(lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();
				} else {
					break;
				}
			} while(true);
		}

		logger.exiting(AcceptEncodingParser.class.getName(), "parse", acceptEncodingList);

		return acceptEncodingList;
	}
}
