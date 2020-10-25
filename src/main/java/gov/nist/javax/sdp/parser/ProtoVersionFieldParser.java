package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import gov.nist.core.*;
import java.text.*;

/**
 * Parser for Proto Version.
 */
public class ProtoVersionFieldParser extends SDPParser {
	public ProtoVersionFieldParser(String protoVersionField) {
		this.lexer = new Lexer("charLexer", protoVersionField);
	}

	public ProtoVersionField protoVersionField() throws ParseException {
		try {
			this.lexer.match('v');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			ProtoVersionField protoVersionField = new ProtoVersionField();

			lexer.match(LexerCore.ID);

			Token version = lexer.getNextToken();

			protoVersionField.setProtoVersion(Integer.parseInt(version.getTokenValue()));

			this.lexer.SPorHT();

			return protoVersionField;
		} catch(Exception e) {
			throw lexer.createParseException();
		}
	}

	public SDPField parse() throws ParseException {
		return this.protoVersionField();
	}
}
