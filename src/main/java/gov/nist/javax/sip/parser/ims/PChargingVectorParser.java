package gov.nist.javax.sip.parser.ims;

import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PChargingVector;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * P-Charging-Vector header parser.
 */
public class PChargingVectorParser extends ParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PChargingVectorParser.class.getName());

	public PChargingVectorParser(String chargingVector) {
		super(chargingVector);
	}

	protected PChargingVectorParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PChargingVectorParser.class.getName(), "parse");

		headerName(TokenTypes.P_VECTOR_CHARGING);
		PChargingVector chargingVector = new PChargingVector();

		while (lexer.lookAhead(0) != '\n') {
			this.parseParameter(chargingVector);
			this.lexer.SPorHT();
			char la = lexer.lookAhead(0);
			if (la == '\n' || la == '\0')
				break;
			this.lexer.match(';');
			this.lexer.SPorHT();
		}

		super.parse(chargingVector);

		if(chargingVector.getParameter(ParameterNamesIms.ICID_VALUE) == null) {
			throw new ParseException("Missing a required Parameter : " + ParameterNamesIms.ICID_VALUE, 0);
		}

		logger.exiting(PChargingVectorParser.class.getName(), "parse", chargingVector);

		return chargingVector;
	}

	protected void parseParameter(PChargingVector chargingVector) throws ParseException {
		logger.entering(PChargingVectorParser.class.getName(), "parseParameter");

		NameValue nv = this.nameValue('=');

		chargingVector.setParameter(nv);

		logger.exiting(PChargingVectorParser.class.getName(), "parseParameter");
	}
}
