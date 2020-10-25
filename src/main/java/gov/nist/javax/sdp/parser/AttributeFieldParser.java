package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.core.NameValue;
import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.SDPField;

public class AttributeFieldParser extends SDPParser {
	public AttributeFieldParser(String attributeField) {
		this.lexer = new Lexer("charLexer", attributeField);
	}

	public AttributeField attributeField() throws ParseException {
		try {
			AttributeField attributeField = new AttributeField();

			this.lexer.match('a');

			this.lexer.SPorHT();
			this.lexer.match('=');

			this.lexer.SPorHT();

			NameValue nameValue = new NameValue();

			int ptr = this.lexer.markInputPosition();

			try {
				String name = lexer.getNextToken(':');

				this.lexer.consume(1);

				String value = lexer.getRest();

				nameValue = new NameValue(name.trim(), value.trim());
			} catch(ParseException ex) {
				this.lexer.rewindInputPosition(ptr);
				String rest = this.lexer.getRest();

				if(null == rest) {
					throw new ParseException(this.lexer.getBuffer(), this.lexer.getPtr());
				}

				nameValue = new NameValue(rest.trim(), null);
			}

			attributeField.setAttribute(nameValue);

			this.lexer.SPorHT();

			return attributeField;
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

	public SDPField parse() throws ParseException {
		return this.attributeField();
	}
}
