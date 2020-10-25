package gov.nist.javax.sip.parser.ims;

import gov.nist.core.Token;
/**
 * Privacy header parser.
 * 
 * Privacy-hdr  =  "Privacy" HCOLON priv-value *(";" priv-value)
 * priv-value   =   "header" / "session" / "user" / "none" / "critical" / token
 */
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import gov.nist.javax.sip.parser.HeaderParser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.Privacy;
import gov.nist.javax.sip.header.ims.PrivacyList;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;

public class PrivacyParser extends HeaderParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PrivacyParser.class.getName());

	public PrivacyParser(String privacyType) {
		super(privacyType);
	}

	protected PrivacyParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PrivacyParser.class.getName(), "parse");

		PrivacyList privacyList = new PrivacyList();

		this.headerName(TokenTypes.PRIVACY);

		while(lexer.lookAhead(0) != '\n') {
			this.lexer.SPorHT();

			Privacy privacy = new Privacy();

			privacy.setHeaderName(SIPHeaderNamesIms.PRIVACY);

			this.lexer.match(TokenTypes.ID);

			Token token = lexer.getNextToken();

			privacy.setPrivacy(token.getTokenValue());

			this.lexer.SPorHT();

			privacyList.add(privacy);

			// Parsing others option-tags
			while(lexer.lookAhead(0) == ';') {
				this.lexer.match(';');
				this.lexer.SPorHT();

				privacy = new Privacy();

				this.lexer.match(TokenTypes.ID);

				token = lexer.getNextToken();
				privacy.setPrivacy(token.getTokenValue());

				this.lexer.SPorHT();

				privacyList.add(privacy);
			}
		}

		return privacyList;
	}
}
