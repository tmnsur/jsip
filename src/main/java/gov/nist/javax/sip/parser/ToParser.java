package gov.nist.javax.sip.parser;

import java.text.ParseException;
import gov.nist.javax.sip.header.*;

/**
 * To Header parser.
 */
public class ToParser extends AddressParametersParser {
	/**
	 * Creates new ToParser
	 * 
	 * @param to String to set
	 */
	public ToParser(String to) {
		super(to);
	}

	protected ToParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		headerName(TokenTypes.TO);

		To to = new To();

		super.parse(to);

		this.lexer.match('\n');

		return to;
	}
}
