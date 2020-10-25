package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.fields.URIField;

/**
 * URI Field Parser.
 */
public class URIFieldParser extends SDPParser {
	public URIFieldParser(String uriField) {
		this.lexer = new Lexer("charLexer", uriField);
	}

	/**
	 * Get the URI field
	 * 
	 * @return URIField
	 */
	public URIField uriField() throws ParseException {
		try {
			this.lexer.match('u');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			URIField uriField = new URIField();
			String rest = lexer.getRest().trim();

			uriField.setURI(rest);

			return uriField;
		} catch(Exception e) {
			throw lexer.createParseException();
		}
	}

	public SDPField parse() throws ParseException {
		return this.uriField();
	}
}
