package gov.nist.javax.sip.parser.extensions;

import java.text.ParseException;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.ReferredBy;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * ReferredBy Header parser.
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * Based on JAIN ReferToParser
 *
 */
public class ReferredByParser extends AddressParametersParser {
	/**
	 * Creates new ToParser
	 * 
	 * @param referBy String to set
	 */
	public ReferredByParser(String referBy) {
		super(referBy);
	}

	protected ReferredByParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		headerName(TokenTypes.REFERREDBY_TO);

		ReferredBy referBy = new ReferredBy();

		super.parse(referBy);

		this.lexer.match('\n');

		return referBy;
	}
}
