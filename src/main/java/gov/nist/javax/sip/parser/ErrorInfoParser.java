package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.address.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for ErrorInfo header.
 */
public class ErrorInfoParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(ErrorInfoParser.class.getName());

	/**
	 * Creates a new instance of ErrorInfoParser
	 * 
	 * @param errorInfo the header to parse
	 */
	public ErrorInfoParser(String errorInfo) {
		super(errorInfo);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected ErrorInfoParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the ErrorInfo String header
	 * 
	 * @return SIPHeader (ErrorInfoList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ErrorInfoParser.class.getName(), "parse");

		ErrorInfoList list = new ErrorInfoList();

		headerName(TokenTypes.ERROR_INFO);

		while (lexer.lookAhead(0) != '\n') {
			do {
				ErrorInfo errorInfo = new ErrorInfo();
				errorInfo.setHeaderName(SIPHeaderNames.ERROR_INFO);

				this.lexer.SPorHT();
				this.lexer.match('<');
				URLParser urlParser = new URLParser((Lexer) this.lexer);
				GenericURI uri = urlParser.uriReference(true);
				errorInfo.setErrorInfo(uri);
				this.lexer.match('>');
				this.lexer.SPorHT();

				super.parse(errorInfo);
				list.add(errorInfo);

				if (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
				} else
					break;
			} while (true);
		}

		logger.exiting(ErrorInfoParser.class.getName(), "parse", list);

		return list;
	}
}
