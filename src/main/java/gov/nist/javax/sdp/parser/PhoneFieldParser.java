package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser for the Phone field.
 */
public class PhoneFieldParser extends SDPParser {
	private static final Logger logger = Logger.getLogger(PhoneFieldParser.class.getName());

	public PhoneFieldParser(String phoneField) {
		lexer = new Lexer("charLexer", phoneField);
	}

	public String getDisplayName(String rest) {
		String retval = null;

		try {
			int begin = rest.indexOf("(");
			int end = rest.indexOf(")");

			if (begin != -1) {
				// p=+44-171-380-7777 (Mark Handley)
				retval = rest.substring(begin + 1, end);
			} else {
				// The alternative RFC822 name quoting convention is
				// also allowed for
				// email addresses. ex: p=Mark Handley <+44-171-380-7777>
				int ind = rest.indexOf("<");
				if (ind != -1) {
					retval = rest.substring(0, ind);
				} else {
					// There is no display name !!!
				}
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return retval;
	}

	public String getPhoneNumber(String rest) throws ParseException {
		String phoneNumber = null;

		try {
			int begin = rest.indexOf("(");

			if (begin != -1) {
				// p=+44-171-380-7777 (Mark Handley)
				phoneNumber = rest.substring(0, begin).trim();
			} else {
				// The alternative RFC822 name quoting convention is also allowed for
				// email addresses. ex: p=Mark Handley <+44-171-380-7777>
				int ind = rest.indexOf("<");
				int end = rest.indexOf(">");

				if (ind != -1) {
					phoneNumber = rest.substring(ind + 1, end);
				} else {
					// p=+44-171-380-7777
					phoneNumber = rest.trim();
				}
			}
		} catch (Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
		return phoneNumber;
	}

	public PhoneField phoneField() throws ParseException {
		try {
			this.lexer.match('p');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			PhoneField phoneField = new PhoneField();
			String rest = lexer.getRest();

			String displayName = getDisplayName(rest.trim());

			phoneField.setName(displayName);

			String phoneNumber = getPhoneNumber(rest);

			phoneField.setPhoneNumber(phoneNumber);

			return phoneField;
		} catch(Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
	}

	@Override
	public SDPField parse() throws ParseException {
		return this.phoneField();
	}
}
