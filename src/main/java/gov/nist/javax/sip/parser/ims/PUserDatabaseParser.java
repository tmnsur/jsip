package gov.nist.javax.sip.parser.ims;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PUserDatabase;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

/**
 * This is the parser for the P-user-database header. The syntax for the
 * P-user-database header as per RFC 4457 is given below:
 *
 * P-User-Database = "P-User-Database" HCOLON database *( SEMI generic-param )
 * database = LAQUOT DiameterURI RAQUOT
 *
 * Eg: P-User-Database: <aaa://host.example.com;transport=tcp>
 */
public class PUserDatabaseParser extends ParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PUserDatabaseParser.class.getName());

	/**
	 *
	 * @param databaseName
	 */
	public PUserDatabaseParser(String databaseName) {
		super(databaseName);
	}

	/**
	 *
	 * @param lexer
	 */
	public PUserDatabaseParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PUserDatabaseParser.class.getName(), "parse");

		this.lexer.match(TokenTypes.P_USER_DATABASE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();

		PUserDatabase userDatabase = new PUserDatabase();

		this.parseheader(userDatabase);

		logger.exiting(PUserDatabaseParser.class.getName(), "parse", userDatabase);

		return userDatabase;
	}

	private void parseheader(PUserDatabase userDatabase) throws ParseException {
		StringBuilder dbname = new StringBuilder();

		this.lexer.match(LESS_THAN);

		while(this.lexer.hasMoreChars()) {
			char next = this.lexer.getNextChar();

			if(next != '>' && next != '\n') {
				dbname.append(next);
			}
		}

		userDatabase.setDatabaseName(dbname.toString());

		super.parse(userDatabase);
	}
}
