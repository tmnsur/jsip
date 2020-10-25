package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.Path;
import gov.nist.javax.sip.header.ims.PathList;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;
import java.util.logging.Logger;

public class PathParser extends AddressParametersParser implements TokenTypes {
	private static final Logger logger = Logger.getLogger(PathParser.class.getName());
	/**
	 * Constructor
	 */
	public PathParser(String path) {
		super(path);
	}

	protected PathParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message and generate the RecordRoute List Object
	 * 
	 * @return SIPHeader the RecordRoute List object
	 * @throws ParseException if errors occur during the parsing
	 */
	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(PathParser.class.getName(), "parse");

		PathList pathList = new PathList();

		this.lexer.match(TokenTypes.PATH);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
		while (true) {
			Path path = new Path();
			super.parse(path);
			pathList.add(path);
			this.lexer.SPorHT();
			char la = lexer.lookAhead(0);
			if (la == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();
			} else if (la == '\n')
				break;
			else
				throw createParseException("unexpected char");
		}

		logger.exiting(PathParser.class.getName(), "parse", pathList);

		return pathList;
	}
}
