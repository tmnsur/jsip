package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import java.text.*;

public class InformationFieldParser extends SDPParser {
	public InformationFieldParser(String informationField) {
		this.lexer = new Lexer("charLexer", informationField);
	}

	public InformationField informationField() throws ParseException {
		try {
			this.lexer.match('i');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			InformationField informationField = new InformationField();
			String rest = lexer.getRest();
			informationField.setInformation(rest.trim());

			return informationField;
		} catch (Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
	}

	public SDPField parse() throws ParseException {
		return this.informationField();
	}
}
