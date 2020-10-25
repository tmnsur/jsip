package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Subject;

/**
 * Parser for Subject header.
 */
public class SubjectParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(SubjectParser.class.getName());

	/**
	 * Creates a new instance of SubjectParser
	 * 
	 * @param subject the header to parse
	 */
	public SubjectParser(String subject) {
		super(subject);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected SubjectParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (Subject object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(SubjectParser.class.getName(), "parse");

		Subject subject = new Subject();

		headerName(TokenTypes.SUBJECT);
		String s = this.lexer.getRest();
		subject.setSubject(s.trim());

		logger.exiting(SubjectParser.class.getName(), "parse", subject);

		return subject;
	}
}
