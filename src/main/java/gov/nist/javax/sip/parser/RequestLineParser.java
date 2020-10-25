package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.*;
import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.*;

/**
 * Parser for the SIP request line.
 */
public class RequestLineParser extends Parser {
	private static final Logger logger = Logger.getLogger(RequestLineParser.class.getName());

	public RequestLineParser(String requestLine) {
		this.lexer = new Lexer("method_keywordLexer", requestLine);
	}

	public RequestLineParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("method_keywordLexer");
	}

	public RequestLine parse() throws ParseException {
		logger.entering(RequestLineParser.class.getName(), "parse");

		RequestLine retval = new RequestLine();
		String m = method();

		lexer.SPorHT();
		retval.setMethod(m);
		this.lexer.selectLexer("sip_urlLexer");
		URLParser urlParser = new URLParser(this.getLexer());
		GenericURI url = urlParser.uriReference(true);
		lexer.SPorHT();
		retval.setUri(url);

		this.lexer.selectLexer("request_lineLexer");
		String v = sipVersion();
		retval.setSipVersion(v);
		lexer.SPorHT();
		lexer.match('\n');

		logger.exiting(RequestLineParser.class.getName(), "parse", retval);

		return retval;
	}
}
