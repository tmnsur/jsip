package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sdp.fields.TypedTime;

public class TimeFieldParser extends SDPParser {
	public TimeFieldParser(String timeField) {
		lexer = new Lexer("charLexer", timeField);
	}

	/**
	 * Get the typed time.
	 *
	 * @param tokenValue to convert to typedTime.
	 * @return TypedTime
	 */
	public TypedTime getTypedTime(String tokenValue) {
		TypedTime typedTime = new TypedTime();

		if(tokenValue.endsWith("d")) {
			typedTime.setUnit("d");
			typedTime.setTime(Integer.parseInt(tokenValue.replace('d', ' ').trim()));
		} else if(tokenValue.endsWith("h")) {
			typedTime.setUnit("h");
			typedTime.setTime(Integer.parseInt(tokenValue.replace('h', ' ').trim()));
		} else if(tokenValue.endsWith("m")) {
			typedTime.setUnit("m");
			typedTime.setTime(Integer.parseInt(tokenValue.replace('m', ' ').trim()));
		} else {
			typedTime.setUnit("s");
			if(tokenValue.endsWith("s")) {
				typedTime.setTime(Integer.parseInt(tokenValue.replace('s', ' ').trim()));
			} else {
				typedTime.setTime(Integer.parseInt(tokenValue.trim()));
			}
		}

		return typedTime;
	}

	private long getTime() throws ParseException {
		try {
			String startTime = this.lexer.number();

			if(startTime.length() > 18) {
				startTime = startTime.substring(startTime.length() - 18);
			}

			return Long.parseLong(startTime);
		} catch(NumberFormatException ex) {
			throw lexer.createParseException();
		}
	}

	/**
	 * parse the field string
	 * 
	 * @return TimeField
	 */
	public TimeField timeField() throws ParseException {
		try {
			this.lexer.match('t');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			TimeField timeField = new TimeField();

			long st = this.getTime();
			timeField.setStartTime(st);
			this.lexer.SPorHT();

			st = this.getTime();
			timeField.setStopTime(st);

			return timeField;
		} catch (Exception e) {
			throw lexer.createParseException();
		}
	}

	public SDPField parse() throws ParseException {
		return this.timeField();
	}
}
