package gov.nist.javax.sdp.parser;

import gov.nist.core.LexerCore;
import gov.nist.core.Separators;

public class Lexer extends LexerCore {
	public Lexer(String lexerName, String buffer) {
		super(lexerName, buffer);

	}

	@Override
	public void selectLexer(String lexerName) {
		// nothing
	}

	public static String getFieldName(String line) {
		int i = line.indexOf(Separators.EQUAL);
		if (i == -1)
			return null;
		else
			return line.substring(0, i);
	}
}
