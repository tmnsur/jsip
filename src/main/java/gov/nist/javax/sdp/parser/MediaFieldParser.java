package gov.nist.javax.sdp.parser;

import java.text.ParseException;
import java.util.Vector;
import java.util.logging.Logger;

import gov.nist.core.LexerCore;
import gov.nist.core.Token;
import gov.nist.javax.sdp.fields.MediaField;
import gov.nist.javax.sdp.fields.SDPField;

/**
 * Parser for Media field.
 */
public class MediaFieldParser extends SDPParser {
	private static final Logger logger = Logger.getLogger(MediaFieldParser.class.getName());

	public MediaFieldParser(String mediaField) {
		lexer = new Lexer("charLexer", mediaField);
	}

	public MediaField mediaField() throws ParseException {
		logger.entering(MediaFieldParser.class.getName(), "mediaField");

		try {
			MediaField mediaField = new MediaField();

			lexer.match('m');
			lexer.SPorHT();
			lexer.match('=');
			lexer.SPorHT();

			lexer.match(LexerCore.ID);

			Token media = lexer.getNextToken();

			mediaField.setMedia(media.getTokenValue());

			this.lexer.SPorHT();

			lexer.match(LexerCore.ID);

			Token port = lexer.getNextToken();

			mediaField.setPort(Integer.parseInt(port.getTokenValue()));

			this.lexer.SPorHT();

			// Some strange media formatting from Sun Ray systems with media
			// reported by Emil Ivov and Iain Macdonnell at Sun
			if(lexer.hasMoreChars() && lexer.lookAhead(1) == '\n') {
				return mediaField;
			}

			if(lexer.lookAhead(0) == '/') {
				// The number of ports is present:
				lexer.consume(1);
				lexer.match(LexerCore.ID);

				Token portsNumber = lexer.getNextToken();

				mediaField.setNports(Integer.parseInt(portsNumber.getTokenValue()));

				this.lexer.SPorHT();
			}

			// proto = token *("/" token)
			lexer.match(LexerCore.ID);

			Token token = lexer.getNextToken();
			String transport = token.getTokenValue();

			while(lexer.lookAhead(0) == '/') {
				lexer.consume(1);
				lexer.match(LexerCore.ID);

				Token transportTemp = lexer.getNextToken();

				transport = transport + "/" + transportTemp.getTokenValue();
			}

			mediaField.setProto(transport);

			this.lexer.SPorHT();

			// The formats list:
			Vector<String> formatList = new Vector<>();
			while(lexer.hasMoreChars()) {
				char la = lexer.lookAhead(0);

				if(la == '\n' || la == '\r') {
					break;
				}

				this.lexer.SPorHT();

				lexer.match(LexerCore.ID);

				Token tok = lexer.getNextToken();

				this.lexer.SPorHT();

				String format = tok.getTokenValue().trim();

				if(!format.equals("")) {
					formatList.add(format);
				}
			}
			mediaField.setFormats(formatList);

			return mediaField;
		} catch(Exception e) {
			throw new ParseException(lexer.getBuffer(), lexer.getPtr());
		} finally {
			logger.exiting(MediaFieldParser.class.getName(), "mediaField");
		}
	}

	public SDPField parse() throws ParseException {
		return this.mediaField();
	}
}
