package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.SIPHeader;

import java.text.ParseException;

/**
 * From header parser.
 */
public class FromParser extends AddressParametersParser {
	public FromParser(String from) {
		super(from);
	}

	protected FromParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		From from = new From();

		headerName(FROM);

		super.parse(from);

		this.lexer.match('\n');

		return from;
	}
}
