package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.AlertInfo;
import gov.nist.javax.sip.header.AlertInfoList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderNames;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for AlertInfo header.
 */
public class AlertInfoParser extends ParametersParser {
	private static final Logger logger = Logger.getLogger(AlertInfoParser.class.getName());

	/**
	 * Creates a new instance of AlertInfo Parser
	 * 
	 * @param alertInfo the header to parse
	 */
	public AlertInfoParser(String alertInfo) {
		super(alertInfo);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected AlertInfoParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the AlertInfo String header
	 * 
	 * @return SIPHeader (AlertInfoList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(AlertInfoParser.class.getName(), "parse");

		AlertInfoList list = new AlertInfoList();

		headerName(TokenTypes.ALERT_INFO);

		int lineCount = 0;
		// Allow only 20 lines in alert-info.
		while((lexer.lookAhead(0) != '\n') && (lineCount < 20)) {
			do {
				AlertInfo alertInfo = new AlertInfo();

				alertInfo.setHeaderName(SIPHeaderNames.ALERT_INFO);

				URLParser urlParser;
				GenericURI uri;

				this.lexer.SPorHT();

				if(this.lexer.lookAhead(0) == '<') {
					this.lexer.match('<');

					urlParser = new URLParser((Lexer) this.lexer);

					uri = urlParser.uriReference(true);

					alertInfo.setAlertInfo(uri);

					this.lexer.match('>');
				} else {
					/*
					 * This is non standard for Polycom support. I know it is bad grammar but please do not remove.
					 */
					alertInfo.setAlertInfo(this.lexer.byteStringNoSemicolon());
				}

				this.lexer.SPorHT();

				super.parse(alertInfo);

				list.add(alertInfo);

				if(lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
				} else {
					break;
				}
			} while(true);

			lineCount++;
		}

		logger.exiting(AlertInfoParser.class.getName(), "parse", list);

		return list;
	}
}
