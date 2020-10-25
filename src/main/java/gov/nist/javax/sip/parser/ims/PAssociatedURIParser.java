package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssociatedURI;
import gov.nist.javax.sip.header.ims.PAssociatedURIList;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * P-Associated-URI header parser
 */
public class PAssociatedURIParser extends AddressParametersParser {
	private static final Logger logger = Logger.getLogger(PAssociatedURIParser.class.getName());

	/**
	 * Constructor
	 * 
	 * @param associatedURI content to set
	 */
	public PAssociatedURIParser(String associatedURI) {
		super(associatedURI);
	}

	protected PAssociatedURIParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PAssociatedURIParser.class.getName(), "parse");

		PAssociatedURIList associatedURIList = new PAssociatedURIList();

		headerName(TokenTypes.P_ASSOCIATED_URI);

		PAssociatedURI associatedURI = new PAssociatedURI();
		associatedURI.setHeaderName(SIPHeaderNamesIms.P_ASSOCIATED_URI);

		super.parse(associatedURI);
		associatedURIList.add(associatedURI);

		this.lexer.SPorHT();
		while (lexer.lookAhead(0) == ',') {
			this.lexer.match(',');
			this.lexer.SPorHT();

			associatedURI = new PAssociatedURI();
			super.parse(associatedURI);
			associatedURIList.add(associatedURI);

			this.lexer.SPorHT();
		}
		this.lexer.SPorHT();
		this.lexer.match('\n');

		logger.exiting(PAssociatedURIParser.class.getName(), "parse", associatedURIList);

		return associatedURIList;
	}
}
