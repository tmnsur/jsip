package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.ims.PMediaAuthorizationList;
import gov.nist.javax.sip.header.ims.PMediaAuthorization;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * P-Media-Authorization header parser.
 */
public class PMediaAuthorizationParser extends HeaderParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PMediaAuthorizationParser.class.getName());

	public PMediaAuthorizationParser(String mediaAuthorization) {
		super(mediaAuthorization);
	}

	public PMediaAuthorizationParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		PMediaAuthorizationList mediaAuthorizationList = new PMediaAuthorizationList();

		logger.entering(PMediaAuthorizationParser.class.getName(), "parse");

		headerName(TokenTypes.P_MEDIA_AUTHORIZATION);

		PMediaAuthorization mediaAuthorization = new PMediaAuthorization();
		mediaAuthorization.setHeaderName(SIPHeaderNamesIms.P_MEDIA_AUTHORIZATION);

		while (lexer.lookAhead(0) != '\n') {
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			try {
				mediaAuthorization.setMediaAuthorizationToken(token.getTokenValue());
			} catch (InvalidArgumentException e) {
				throw createParseException(e.getMessage());
			}
			mediaAuthorizationList.add(mediaAuthorization);

			this.lexer.SPorHT();
			if (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				mediaAuthorization = new PMediaAuthorization();
			}
			this.lexer.SPorHT();
		}

		logger.exiting(PMediaAuthorizationParser.class.getName(), "parse", mediaAuthorizationList);

		return mediaAuthorizationList;
	}
}
