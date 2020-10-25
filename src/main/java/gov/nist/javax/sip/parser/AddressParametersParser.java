package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.address.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Address parameters parser.
 */
public class AddressParametersParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(AddressParametersParser.class.getName());

	protected AddressParametersParser(Lexer lexer) {
		super(lexer);
	}

	protected AddressParametersParser(String buffer) {
		super(buffer);
	}

	protected void parse(AddressParametersHeader addressParametersHeader) throws ParseException {
		logger.entering(AddressParametersParser.class.getName(), "parse");

		AddressParser addressParser = new AddressParser(this.getLexer());
		AddressImpl addr = addressParser.address(false);

		addressParametersHeader.setAddress(addr);

		lexer.SPorHT();

		char la = this.lexer.lookAhead(0);

		if(this.lexer.hasMoreChars() && la != '\0' && la != '\n' && this.lexer.startsId()) {
			super.parseNameValueList(addressParametersHeader);
		} else {
			super.parse(addressParametersHeader);
		}

		logger.exiting(AddressParametersParser.class.getName(), "parse");
	}
}
