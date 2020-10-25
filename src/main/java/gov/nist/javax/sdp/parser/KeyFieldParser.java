package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import gov.nist.core.*;
import java.text.*;

/**
 * Parser for key field.
 */
public class KeyFieldParser extends SDPParser {
	public KeyFieldParser(String keyField) {
		this.lexer = new Lexer("charLexer", keyField);
	}

	public KeyField keyField() throws ParseException {
		try {
			this.lexer.match('k');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			KeyField keyField = new KeyField();

			// Stealing the approach from AttributeFieldParser from from here...
			NameValue nameValue = new NameValue();

			int ptr = this.lexer.markInputPosition();
			try {
				String name = lexer.getNextToken(':');
				this.lexer.consume(1);
				String value = lexer.getRest();
				nameValue = new NameValue(name.trim(), value.trim());
			} catch (ParseException ex) {
				this.lexer.rewindInputPosition(ptr);
				String rest = this.lexer.getRest();
				if (rest == null)
					throw new ParseException(this.lexer.getBuffer(), this.lexer.getPtr());
				nameValue = new NameValue(rest.trim(), null);
			}

			keyField.setType(nameValue.getName());
			keyField.setKeyData((String) nameValue.getValueAsObject());

			this.lexer.SPorHT();

			return keyField;
		} catch (Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
	}

	@Override
	public SDPField parse() throws ParseException {
		return this.keyField();
	}
}
