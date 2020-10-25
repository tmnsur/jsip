package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Parser for Organization header.
 */
public class OrganizationParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(OrganizationParser.class.getName());

	/**
	 * Creates a new instance of OrganizationParser
	 * 
	 * @param organization the header to parse
	 */
	public OrganizationParser(String organization) {
		super(organization);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected OrganizationParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String header
	 * 
	 * @return SIPHeader (Organization object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(OrganizationParser.class.getName(), "parse");

		Organization organization = new Organization();

		headerName(TokenTypes.ORGANIZATION);

		String value = this.lexer.getRest();

		organization.setOrganization(value.trim());

		logger.exiting(OrganizationParser.class.getName(), "parse", organization);

		return organization;
	}
}
