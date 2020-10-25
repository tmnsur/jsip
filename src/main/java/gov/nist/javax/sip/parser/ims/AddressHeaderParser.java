package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.parser.AddressParser;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.ims.AddressHeaderIms;

abstract class AddressHeaderParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(AddressHeaderParser.class.getName());

	protected AddressHeaderParser(Lexer lexer) {
		super(lexer);
	}

	protected AddressHeaderParser(String buffer) {
		super(buffer);
	}

	protected void parse(AddressHeaderIms addressHeader) throws ParseException {
		logger.entering(AddressHeaderParser.class.getName(), "parse");

		AddressParser addressParser = new AddressParser(this.getLexer());
		AddressImpl addr = addressParser.address(true);

		addressHeader.setAddress(addr);

		logger.exiting(AddressHeaderParser.class.getName(), "parse");
	}
}
