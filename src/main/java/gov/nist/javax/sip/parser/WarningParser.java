package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for Warning header.
 */
public class WarningParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(WarningParser.class.getName());

	/**
	 * Constructor
	 *
	 * @param warning - Warning header to parse
	 */
	public WarningParser(String warning) {
		super(warning);
	}

	/**
	 * Constructor
	 *
	 * @param lexer - the lexer to use.
	 */
	protected WarningParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 *
	 * @return SIPHeader (WarningList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(WarningParser.class.getName(), "parse");

		WarningList warningList = new WarningList();

		headerName(TokenTypes.WARNING);

		while (lexer.lookAhead(0) != '\n') {
			Warning warning = new Warning();
			warning.setHeaderName(SIPHeaderNames.WARNING);

			// Parsing the 3digits code
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			try {
				int code = Integer.parseInt(token.getTokenValue());
				warning.setCode(code);
			} catch (NumberFormatException ex) {
				throw createParseException(ex.getMessage());
			} catch (InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}
			this.lexer.SPorHT();

			// Parsing the agent
			this.lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			// Bug reported by zvali@dev.java.net
			if (lexer.lookAhead(0) == ':') {
				this.lexer.match(':');
				this.lexer.match(TokenTypes.ID);
				Token token2 = lexer.getNextToken();
				warning.setAgent(token.getTokenValue() + ":" + token2.getTokenValue());
			} else {
				warning.setAgent(token.getTokenValue());
			}

			this.lexer.SPorHT();

			// Parsing the text
			String text = this.lexer.quotedString();
			warning.setText(text);
			this.lexer.SPorHT();

			warningList.add(warning);

			while (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				warning = new Warning();

				// Parsing the 3digits code
				this.lexer.match(TokenTypes.ID);
				Token tok = lexer.getNextToken();
				try {
					int code = Integer.parseInt(tok.getTokenValue());
					warning.setCode(code);
				} catch (NumberFormatException ex) {
					throw createParseException(ex.getMessage());
				} catch (InvalidArgumentException ex) {
					throw createParseException(ex.getMessage());
				}
				this.lexer.SPorHT();

				// Parsing the agent
				this.lexer.match(TokenTypes.ID);
				tok = lexer.getNextToken();

				if (lexer.lookAhead(0) == ':') {
					this.lexer.match(':');
					this.lexer.match(TokenTypes.ID);
					Token token2 = lexer.getNextToken();
					warning.setAgent(tok.getTokenValue() + ":" + token2.getTokenValue());
				} else {
					warning.setAgent(tok.getTokenValue());
				}

				this.lexer.SPorHT();

				// Parsing the text
				text = this.lexer.quotedString();
				warning.setText(text);
				this.lexer.SPorHT();

				warningList.add(warning);
			}
		}

		logger.exiting(WarningParser.class.getName(), "parse", warningList);

		return warningList;
	}
}
