package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.address.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for CallInfo header.
 */
public class CallInfoParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(CallInfoParser.class.getName());

	/**
	 * Creates a new instance of CallInfoParser
	 * 
	 * @param callInfo the header to parse
	 */
	public CallInfoParser(String callInfo) {
		super(callInfo);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected CallInfoParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the CallInfo String header
	 * 
	 * @return SIPHeader (CallInfoList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(CallInfoParser.class.getName(), "parse");

		CallInfoList list = new CallInfoList();

		headerName(TokenTypes.CALL_INFO);

		while (lexer.lookAhead(0) != '\n') {
			CallInfo callInfo = new CallInfo();
			callInfo.setHeaderName(SIPHeaderNames.CALL_INFO);

			this.lexer.SPorHT();
			this.lexer.match('<');
			URLParser urlParser = new URLParser((Lexer) this.lexer);
			GenericURI uri = urlParser.uriReference(true);
			callInfo.setInfo(uri);
			this.lexer.match('>');
			this.lexer.SPorHT();

			super.parse(callInfo);
			list.add(callInfo);

			while (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				callInfo = new CallInfo();

				this.lexer.SPorHT();
				this.lexer.match('<');
				urlParser = new URLParser((Lexer) this.lexer);
				uri = urlParser.uriReference(true);
				callInfo.setInfo(uri);
				this.lexer.match('>');
				this.lexer.SPorHT();

				super.parse(callInfo);
				list.add(callInfo);
			}
		}

		logger.exiting(CallInfoParser.class.getName(), "parse", list);

		return list;
	}
}
