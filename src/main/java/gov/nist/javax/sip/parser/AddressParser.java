package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for addresses.
 */
public class AddressParser extends Parser {
	private static final Logger logger = Logger.getLogger(AddressParser.class.getName());

	public AddressParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("charLexer");
	}

	public AddressParser(String address) {
		this.lexer = new Lexer("charLexer", address);
	}

	protected AddressImpl nameAddr() throws ParseException {
		logger.entering(AddressParser.class.getName(), "nameAddr");

		AddressImpl addr;
		if(this.lexer.lookAhead(0) == '<') {
			this.lexer.consume(1);
			this.lexer.selectLexer("sip_urlLexer");
			this.lexer.SPorHT();
			URLParser uriParser = new URLParser((Lexer) lexer);
			GenericURI uri = uriParser.uriReference(true);

			addr = new AddressImpl();
			addr.setAddressType(AddressImpl.NAME_ADDR);
			addr.setURI(uri);

			this.lexer.SPorHT();
			this.lexer.match('>');
		} else {
			addr = new AddressImpl();
			addr.setAddressType(AddressImpl.NAME_ADDR);

			String name = null;

			if(this.lexer.lookAhead(0) == '\"') {
				name = this.lexer.quotedString();

				this.lexer.SPorHT();
			} else {
				name = this.lexer.getNextToken('<');
			}

			addr.setDisplayName(name.trim());

			this.lexer.match('<');
			this.lexer.SPorHT();

			URLParser uriParser = new URLParser((Lexer) lexer);
			GenericURI uri = uriParser.uriReference(true);

			addr.setAddressType(AddressImpl.NAME_ADDR);
			addr.setURI(uri);

			this.lexer.SPorHT();
			this.lexer.match('>');
		}

		logger.exiting(AddressParser.class.getName(), "nameAddr", addr);

		return addr;
	}

	public AddressImpl address(boolean inclParams) throws ParseException {
		logger.entering(AddressParser.class.getName(), "address");

		AddressImpl retval = null;
		int k = 0;
		while(lexer.hasMoreChars()) {
			char la = lexer.lookAhead(k);

			if(la == '<' || la == '\"' || la == ':' || la == '/') {
				break;
			}

			if(la == '\0') {
				throw createParseException("unexpected EOL");
			}

			k++;
		}

		char la = lexer.lookAhead(k);

		if(la == '<' || la == '\"') {
			retval = nameAddr();
		} else if(la == ':' || la == '/') {
			retval = new AddressImpl();

			URLParser uriParser = new URLParser((Lexer) lexer);
			GenericURI uri = uriParser.uriReference(inclParams);

			retval.setAddressType(AddressImpl.ADDRESS_SPEC);
			retval.setURI(uri);
		} else {
			throw createParseException("Bad address spec");
		}

		logger.exiting(AddressParser.class.getName(), "address", retval);

		return retval;
	}
}
