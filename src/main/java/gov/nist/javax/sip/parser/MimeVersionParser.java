package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.sip.*;

/**
 * Parser for MimeVersion header.
 */
public class MimeVersionParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(MimeVersionParser.class.getName());

	/**
	 * Creates a new instance of MimeVersionParser
	 * 
	 * @param mimeVersion the header to parse
	 */
	public MimeVersionParser(String mimeVersion) {
		super(mimeVersion);
	}

	/**
	 * Constructor
	 * 
	 * @param lexer the lexer to use to parse the header
	 */
	protected MimeVersionParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * 
	 * @return SIPHeader (MimeVersion object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(MimeVersionParser.class.getName(), "parse");

		MimeVersion mimeVersion = new MimeVersion();

		headerName(TokenTypes.MIME_VERSION);

		mimeVersion.setHeaderName(SIPHeaderNames.MIME_VERSION);

		try {
			String majorVersion = this.lexer.number();

			mimeVersion.setMajorVersion(Integer.parseInt(majorVersion));

			this.lexer.match('.');

			String minorVersion = this.lexer.number();

			mimeVersion.setMinorVersion(Integer.parseInt(minorVersion));
		} catch(InvalidArgumentException ex) {
			throw createParseException(ex.getMessage());
		}

		this.lexer.SPorHT();

		this.lexer.match('\n');

		logger.exiting(MimeVersionParser.class.getName(), "parse", mimeVersion);

		return mimeVersion;
	}
}
