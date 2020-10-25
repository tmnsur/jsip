package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AcceptLanguage;
import gov.nist.javax.sip.header.AcceptLanguageList;
import gov.nist.javax.sip.header.SIPHeader;

/**
 * Parser for Accept Language Headers.
 *
 * Accept Language body.
 * 
 * <pre>
 *
 * Accept-Language = "Accept-Language" ":"
 *                         1#( language-range [ ";" "q" "=" qvalue ] )
 *       language-range  = ( ( 1*8ALPHA *( "-" 1*8ALPHA ) ) | "*" )
 *
 * HTTP RFC 2616 Section 14.4
 * </pre>
 *
 * Accept-Language: da, en-gb;q=0.8, en;q=0.7
 *
 * @see AcceptLanguageList
 * @version 1.2 $Revision: 1.8 $ $Date: 2009-07-17 18:57:56 $
 */
public class AcceptLanguageParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(AcceptLanguageParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param acceptLanguage AcceptLanguage message to parse
	 */
	public AcceptLanguageParser(String acceptLanguage) {
		super(acceptLanguage);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer Lexer to set
	 */
	protected AcceptLanguageParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (AcceptLanguage object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AcceptLanguageParser.class.getName(), "parse");

		AcceptLanguageList acceptLanguageList = new AcceptLanguageList();

		headerName(TokenTypes.ACCEPT_LANGUAGE);
		do {
			AcceptLanguage acceptLanguage = new AcceptLanguage();

			this.lexer.SPorHT();

			if(lexer.startsId()) {
				// Content-Coding:
				Token value = lexer.match(TokenTypes.ID); // e.g. "en-gb" or '*'

				acceptLanguage.setLanguageRange(value.getTokenValue());

				this.lexer.SPorHT();

				while(lexer.lookAhead(0) == ';') {
					this.lexer.match(';');
					this.lexer.SPorHT();
					this.lexer.match('q');
					this.lexer.SPorHT();
					this.lexer.match('=');
					this.lexer.SPorHT();

					lexer.match(TokenTypes.ID);

					value = lexer.getNextToken();

					try {
						float fl = Float.parseFloat(value.getTokenValue());
						acceptLanguage.setQValue(fl);
					} catch (NumberFormatException | InvalidArgumentException ex) {
						throw createParseException(ex.getMessage());
					}

					this.lexer.SPorHT();
				}
			}

			acceptLanguageList.add(acceptLanguage);

			if(lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else {
				break;
			}
		} while(true);

		return acceptLanguageList;
	}
}
