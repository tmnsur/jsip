package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.core.LexerCore;
import gov.nist.core.Token;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.fields.TypedTime;
import gov.nist.javax.sdp.fields.ZoneAdjustment;
import gov.nist.javax.sdp.fields.ZoneField;

/**
 * Parser For the Zone field.
 */
public class ZoneFieldParser extends SDPParser {
	public ZoneFieldParser(String zoneField) {
		lexer = new Lexer("charLexer", zoneField);
	}

	/**
	 * Get the sign of the offset.
	 *
	 * @param tokenValue to examine.
	 * @return the sign.
	 */
	public String getSign(String tokenValue) {
		if(tokenValue.startsWith("-")) {
			return "-";
		}

		return "+";
	}

	/**
	 * Get the typed time.
	 *
	 * @param tokenValue -- token value to convert to typed time.
	 * @return TypedTime -- the converted typed time value.
	 */
	public TypedTime getTypedTime(String tokenValue) {
		TypedTime typedTime = new TypedTime();
		String offset = null;

		if(tokenValue.startsWith("-")) {
			offset = tokenValue.replace('-', ' ');
		} else if(tokenValue.startsWith("+")) {
			offset = tokenValue.replace('+', ' ');
		} else {
			offset = tokenValue;
		}

		if(offset.endsWith("d")) {
			typedTime.setUnit("d");
			typedTime.setTime(Integer.parseInt(offset.replace('d', ' ').trim()));
		} else if (offset.endsWith("h")) {
			typedTime.setUnit("h");
			typedTime.setTime(Integer.parseInt(offset.replace('h', ' ').trim()));
		} else if (offset.endsWith("m")) {
			typedTime.setUnit("m");
			typedTime.setTime(Integer.parseInt(offset.replace('m', ' ').trim()));
		} else {
			typedTime.setUnit("s");
			if(offset.endsWith("s")) {
				typedTime.setTime(Integer.parseInt(offset.replace('s', ' ').trim()));
			} else {
				typedTime.setTime(Integer.parseInt(offset.trim()));
			}
		}

		return typedTime;
	}

	/**
	 * parse the Zone field string
	 *
	 * @return ZoneField
	 */
	public ZoneField zoneField() throws ParseException {
		try {
			ZoneField zoneField = new ZoneField();

			this.lexer.match('z');
			this.lexer.SPorHT();
			this.lexer.match('=');
			this.lexer.SPorHT();

			// The zoneAdjustment list:
			// Patch 117
			while (lexer.hasMoreChars()) {
				char la = lexer.lookAhead(0);

				if(la == '\n' || la == '\r') {
					break;
				}

				ZoneAdjustment zoneAdjustment = new ZoneAdjustment();

				lexer.match(LexerCore.ID);
				Token time = lexer.getNextToken();

				this.lexer.SPorHT();

				String timeValue = time.getTokenValue();
				if(timeValue.length() > 18) {
					timeValue = timeValue.substring(timeValue.length() - 18);
				}

				zoneAdjustment.setTime(Long.parseLong(timeValue));

				lexer.match(LexerCore.ID);
				Token offset = lexer.getNextToken();
				this.lexer.SPorHT();
				String sign = getSign(offset.getTokenValue());
				TypedTime typedTime = getTypedTime(offset.getTokenValue());
				zoneAdjustment.setSign(sign);
				zoneAdjustment.setOffset(typedTime);

				zoneField.addZoneAdjustment(zoneAdjustment);
			}

			return zoneField;
		} catch (Exception e) {
			throw lexer.createParseException();
		}
	}

	@Override
	public SDPField parse() throws ParseException {
		return this.zoneField();
	}
}
