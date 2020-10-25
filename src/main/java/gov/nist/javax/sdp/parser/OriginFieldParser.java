package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import gov.nist.core.*;
import java.text.*;

public class OriginFieldParser extends SDPParser {
	public OriginFieldParser(String originField) {
		lexer = new Lexer("charLexer", originField);
	}

	public OriginField originField() throws ParseException {
		try {
			OriginField originField = new OriginField();

			lexer.match('o');
			lexer.SPorHT();
			lexer.match('=');
			lexer.SPorHT();

			lexer.match(LexerCore.ID_NO_WHITESPACE);
			Token userName = lexer.getNextToken();
			originField.setUsername(userName.getTokenValue());
			this.lexer.SPorHT();

			lexer.match(LexerCore.ID);

			Token sessionId = lexer.getNextToken();

			// guard against very long session IDs
			String sessId = sessionId.getTokenValue();
			try {
				originField.setSessId(Long.parseLong(sessId));
			} catch (NumberFormatException ex) {
				if (sessId.length() > 18)
					sessId = sessId.substring(sessId.length() - 18);
				originField.setSessionId(sessId);
			}
			this.lexer.SPorHT();

			lexer.match(LexerCore.ID);

			Token sessionVersion = lexer.getNextToken();

			// guard against very long sessionVersion
			String sessVer = sessionVersion.getTokenValue();

			try {
				originField.setSessVersion(Long.parseLong(sessVer));
			} catch (NumberFormatException ex) {
				if (sessVer.length() > 18)
					sessVer = sessVer.substring(sessVer.length() - 18);
				originField.setSessVersion(sessVer);

			}
			this.lexer.SPorHT();

			lexer.match(LexerCore.ID);
			Token networkType = lexer.getNextToken();
			originField.setNettype(networkType.getTokenValue());
			this.lexer.SPorHT();

			lexer.match(LexerCore.ID);
			Token addressType = lexer.getNextToken();
			originField.setAddrtype(addressType.getTokenValue());
			this.lexer.SPorHT();

			String host = lexer.getRest();
			HostNameParser hostNameParser = new HostNameParser(host);
			Host h = hostNameParser.host();
			originField.setAddress(h);

			return originField;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		}
	}

	public SDPField parse() throws ParseException {
		return this.originField();
	}
}
