package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.fields.SessionNameField;

public class SessionNameFieldParser extends SDPParser {
	public SessionNameFieldParser(String sessionNameField) {
		this.lexer = new Lexer("charLexer", sessionNameField);
	}

	/**
	 * Get the SessionNameField
	 * 
	 * @return SessionNameField
	 */
	public SessionNameField sessionNameField() throws ParseException {
		try {
			this.lexer.match('s');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			SessionNameField sessionNameField = new SessionNameField();
			String rest = lexer.getRest();

			// Some endpoints may send us a blank session name ("s=")
			sessionNameField.setSessionName(rest == null ? "" : rest.trim());

			return sessionNameField;
		} catch(Exception e) {
			throw lexer.createParseException();
		}
	}

	public SDPField parse() throws ParseException {
		return this.sessionNameField();
	}
}
