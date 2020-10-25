package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.core.NameValue;
import gov.nist.javax.sdp.fields.BandwidthField;
import gov.nist.javax.sdp.fields.SDPField;

public class BandwidthFieldParser extends SDPParser {
	public BandwidthFieldParser(String bandwidthField) {
		this.lexer = new Lexer("charLexer", bandwidthField);
	}

	public BandwidthField bandwidthField() throws ParseException {
		try {
			this.lexer.match('b');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			BandwidthField bandwidthField = new BandwidthField();

			NameValue nameValue = nameValue(':');
			String name = nameValue.getName();
			String value = (String) nameValue.getValueAsObject();

			bandwidthField.setBandwidth(Integer.parseInt(value.trim()));
			bandwidthField.setBwtype(name);

			this.lexer.SPorHT();
			return bandwidthField;
		} catch(Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
	}

	public SDPField parse() throws ParseException {
		return this.bandwidthField();
	}
}
