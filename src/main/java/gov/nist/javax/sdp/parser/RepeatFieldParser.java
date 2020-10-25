package gov.nist.javax.sdp.parser;

import gov.nist.javax.sdp.fields.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Repeat field.
 */
public class RepeatFieldParser extends SDPParser {
	public RepeatFieldParser(String repeatField) {
		lexer = new Lexer("charLexer", repeatField);
	}

	/**
	 * Get the typed time.
	 * 
	 * @param tokenValue to convert into a typed time.
	 * @return the typed time
	 */
	public TypedTime getTypedTime(String tokenValue) {
		TypedTime typedTime = new TypedTime();

		if (tokenValue.endsWith("d")) {
			typedTime.setUnit("d");
			String t = tokenValue.replace('d', ' ');

			typedTime.setTime(Integer.parseInt(t.trim()));
		} else if (tokenValue.endsWith("h")) {
			typedTime.setUnit("h");
			String t = tokenValue.replace('h', ' ');
			typedTime.setTime(Integer.parseInt(t.trim()));
		} else if (tokenValue.endsWith("m")) {
			typedTime.setUnit("m");
			String t = tokenValue.replace('m', ' ');
			typedTime.setTime(Integer.parseInt(t.trim()));
		} else {
			typedTime.setUnit("s");
			if (tokenValue.endsWith("s")) {
				String t = tokenValue.replace('s', ' ');
				typedTime.setTime(Integer.parseInt(t.trim()));
			} else
				typedTime.setTime(Integer.parseInt(tokenValue.trim()));
		}
		return typedTime;
	}

	/**
	 * parse the field string
	 * 
	 * @return RepeatFields
	 */
	public RepeatField repeatField() throws ParseException {
		try {

			this.lexer.match('r');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			RepeatField repeatField = new RepeatField();

			lexer.match(LexerCore.ID);
			Token repeatInterval = lexer.getNextToken();
			this.lexer.SPorHT();
			TypedTime typedTime = getTypedTime(repeatInterval.getTokenValue());
			repeatField.setRepeatInterval(typedTime);

			lexer.match(LexerCore.ID);
			Token activeDuration = lexer.getNextToken();
			this.lexer.SPorHT();
			typedTime = getTypedTime(activeDuration.getTokenValue());
			repeatField.setActiveDuration(typedTime);

			// The offsets list:
			/* Patch 117 */
			while (lexer.hasMoreChars()) {
				char la = lexer.lookAhead(0);
				if (la == '\n' || la == '\r')
					break;
				lexer.match(LexerCore.ID);
				Token offsets = lexer.getNextToken();
				this.lexer.SPorHT();
				typedTime = getTypedTime(offsets.getTokenValue());
				repeatField.addOffset(typedTime);
			}

			return repeatField;
		} catch (Exception e) {
			throw lexer.createParseException();
		}
	}

	@Override
	public SDPField parse() throws ParseException {
		return this.repeatField();
	}
}
