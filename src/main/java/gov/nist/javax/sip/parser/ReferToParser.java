package gov.nist.javax.sip.parser;

import java.text.ParseException;
import gov.nist.javax.sip.header.*;

/**
 * To Header parser.
 */
public class ReferToParser extends AddressParametersParser {
	/**
	 * Creates new ToParser
	 * 
	 * @param referTo String to set
	 */
	public ReferToParser(String referTo) {
		super(referTo);
	}

	protected ReferToParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		headerName(TokenTypes.REFER_TO);

		ReferTo referTo = new ReferTo();

		super.parse(referTo);

		this.lexer.match('\n');

		return referTo;
	}
}
