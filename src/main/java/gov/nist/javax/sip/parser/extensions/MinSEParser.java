package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.parser.*;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for SIP MinSE Parser.
 *
 * Min-SE = "Min-SE" HCOLON delta-seconds *(SEMI generic-param)
 *
 * @author P. Musgrave <pmusgrave@newheights.com>
 *
 *         <a href="{@docRoot}/uncopyright.html">This code is in the public
 *         domain.</a>
 */
public class MinSEParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(MinSEParser.class.getName());

	/**
	 * protected constructor.
	 * 
	 * @param text is the text of the header to parse
	 */
	public MinSEParser(String text) {
		super(text);
	}

	/**
	 * constructor.
	 * 
	 * @param lexer is the lexer passed in from the enclosing parser.
	 */
	protected MinSEParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Parse the header.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		MinSE minse = new MinSE();

		logger.entering(MinSEParser.class.getName(), "parse");

		try {
			headerName(TokenTypes.MINSE_TO);

			String nextId = lexer.getNextId();
			try {
				int delta = Integer.parseInt(nextId);
				minse.setExpires(delta);
			} catch (NumberFormatException ex) {
				throw createParseException("bad integer format");
			} catch (InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}

			this.lexer.SPorHT();

			super.parse(minse);

			return minse;
		} finally {
			logger.exiting(MinSEParser.class.getName(), "parse", minse);
		}
	}
}
