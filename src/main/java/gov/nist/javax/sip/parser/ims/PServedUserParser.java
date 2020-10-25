package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PServedUser;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;

public class PServedUserParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(PServedUserParser.class.getName());

	protected PServedUserParser(Lexer lexer) {
		super(lexer);
	}

	public PServedUserParser(String servedUser) {
		super(servedUser);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PServedUserParser.class.getName(), "parse");

		PServedUser servedUser = new PServedUser();

		headerName(P_SERVED_USER);

		super.parse(servedUser);
		this.lexer.match('\n');

		logger.exiting(PServedUserParser.class.getName(), "parse", servedUser);

		return servedUser;
	}
}
