package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;
import gov.nist.javax.sip.header.ims.PAssertedIdentityList;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;

import gov.nist.javax.sip.parser.AddressParametersParser;

public class PAssertedIdentityParser extends AddressParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PAssertedIdentityParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param assertedIdentity - message to parse to set
	 */
	public PAssertedIdentityParser(String assertedIdentity) {
		super(assertedIdentity);
	}

	protected PAssertedIdentityParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PAssertedIdentityParser.class.getName(), "parse");

		PAssertedIdentityList assertedIdList = new PAssertedIdentityList();

		headerName(TokenTypes.P_ASSERTED_IDENTITY);

		PAssertedIdentity pai = new PAssertedIdentity();
		pai.setHeaderName(SIPHeaderNamesIms.P_ASSERTED_IDENTITY);

		super.parse(pai);
		assertedIdList.add(pai);

		this.lexer.SPorHT();
		while (lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			pai = new PAssertedIdentity();
			super.parse(pai);
			assertedIdList.add(pai);

			this.lexer.SPorHT();
		}

		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.entering(PAssertedIdentityParser.class.getName(), "parse", assertedIdList);

		return assertedIdList;
	}
}
