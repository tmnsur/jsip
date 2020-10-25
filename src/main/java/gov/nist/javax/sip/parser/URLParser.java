package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
import gov.nist.core.HostPort;
import gov.nist.core.LexerCore;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.StringTokenizer;
import gov.nist.core.Token;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelURLImpl;
import gov.nist.javax.sip.address.TelephoneNumber;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser For SIP and Tel URLs. Other kinds of URL's are handled by the J2SE 1.4
 * URL class.
 */
public class URLParser extends Parser {
	private static final Logger logger = Logger.getLogger(URLParser.class.getName());

	private static final String ESCAPED = "escaped";
	private static final String CHAR_LEXER = "charLexer";

	public URLParser(String url) {
		this.lexer = new Lexer("sip_urlLexer", url);
	}

	public URLParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("sip_urlLexer");
	}

	protected static boolean isMark(char next) {
		switch (next) {
		case '-':
		case '_':
		case '.':
		case '!':
		case '~':
		case '*':
		case '\'':
		case '(':
		case ')':
			return true;
		default:
			return false;
		}
	}

	protected static boolean isUnreserved(char next) {
		return StringTokenizer.isAlphaDigit(next) || isMark(next);
	}

	protected static boolean isReservedNoSlash(char next) {
		switch (next) {
		case ';':
		case '?':
		case ':':
		case '@':
		case '&':
		case '+':
		case '$':
		case ',':
			return true;
		default:
			return false;
		}
	}

	protected static boolean isUserUnreserved(char la) {
		switch (la) {
		case '&':
		case '?':
		case '+':
		case '$':
		case '#':
		case '/':
		case ',':
		case ';':
		case '=':
			return true;
		default:
			return false;
		}
	}

	protected String unreserved() throws ParseException {
		char next = lexer.lookAhead(0);
		if (isUnreserved(next)) {
			lexer.consume(1);
			return String.valueOf(next);
		} else
			throw createParseException("unreserved");

	}

	/**
	 * Name or value of a parameter.
	 */
	protected String paramNameOrValue() {
		int startIdx = lexer.getPtr();
		while(lexer.hasMoreChars()) {
			char next = lexer.lookAhead(0);
			boolean isValidChar;

			switch(next) {
			case '[':
			case ']':
			case '/':
			case ':':
			case '&':
			case '+':
			case '$':
				isValidChar = true;
				break;
			default:
				isValidChar = false;
				break;
			}

			if(isValidChar || isUnreserved(next)) {
				lexer.consume(1);
			} else if(isEscaped()) {
				lexer.consume(3);
			} else {
				break;
			}
		}

		return lexer.getBuffer().substring(startIdx, lexer.getPtr());
	}

	private NameValue uriParam() {
		logger.entering(URLParser.class.getName(), "uriParam");

		String pvalue = "";
		String pname = paramNameOrValue();
		char next = lexer.lookAhead(0);
		boolean isFlagParam = true;
		if (next == '=') {
			lexer.consume(1);

			pvalue = paramNameOrValue();

			isFlagParam = false;
		}

		NameValue nameValue;
		if(pname.length() == 0 && (pvalue == null || pvalue.length() == 0)) {
			nameValue = null;
		} else {
			nameValue = new NameValue(pname, pvalue, isFlagParam);
		}

		logger.exiting(URLParser.class.getName(), "uriParam", nameValue);

		return nameValue;
	}

	protected static boolean isReserved(char next) {
		switch (next) {
		case ';':
		case '/':
		case '?':
		case ':':
		case '=': // Bug fix by Bruno Konik
		case '@':
		case '&':
		case '+':
		case '$':
		case ',':
			return true;
		default:
			return false;
		}
	}

	protected String reserved() throws ParseException {
		char next = lexer.lookAhead(0);
		if (isReserved(next)) {
			lexer.consume(1);
			return new StringBuilder().append(next).toString();
		} else
			throw createParseException("reserved");
	}

	protected boolean isEscaped() {
		try {
			return lexer.lookAhead(0) == '%' && StringTokenizer.isHexDigit(lexer.lookAhead(1))
					&& StringTokenizer.isHexDigit(lexer.lookAhead(2));
		} catch(Exception ex) {
			return false;
		}
	}

	protected String escaped() throws ParseException {
		logger.entering(URLParser.class.getName(), ESCAPED);

		StringBuilder retval = new StringBuilder();
		char next = lexer.lookAhead(0);
		char next1 = lexer.lookAhead(1);
		char next2 = lexer.lookAhead(2);

		if(next == '%' && StringTokenizer.isHexDigit(next1) && StringTokenizer.isHexDigit(next2)) {
			lexer.consume(3);

			retval.append(next);
			retval.append(next1);
			retval.append(next2);
		} else {
			throw createParseException(ESCAPED);
		}

		logger.exiting(URLParser.class.getName(), ESCAPED, retval);

		return retval.toString();
	}

	protected String mark() throws ParseException {
		logger.entering(URLParser.class.getName(), "mark");

		char next = lexer.lookAhead(0);
		if(isMark(next)) {
			lexer.consume(1);

			String result = new String(new char[] { next });

			logger.exiting(URLParser.class.getName(), "mark", result);

			return result;
		}

		throw createParseException("mark");
	}

	protected String uric() {
		logger.entering(URLParser.class.getName(), "uric");

		String result = null;
		try {
			char la = lexer.lookAhead(0);

			if(isUnreserved(la) || isReserved(la)) {
				lexer.consume(1);

				result = LexerCore.charAsString(la);
			} else if(isEscaped()) {
				String retval = lexer.charAsString(3);

				lexer.consume(3);

				result = retval;
			}
		} catch(Exception ex) {
			logger.log(Level.FINEST, "silently ignoring exception", ex);
		}

		logger.exiting(URLParser.class.getName(), "uric", result);

		return result;
	}

	protected String uricNoSlash() {
		logger.entering(URLParser.class.getName(), "uricNoSlash");

		char la = lexer.lookAhead(0);

		String result = null;
		if(isEscaped()) {
			String retval = lexer.charAsString(3);

			lexer.consume(3);

			result = retval;
		} else if(isUnreserved(la) || isReservedNoSlash(la)) {
			lexer.consume(1);

			result = LexerCore.charAsString(la);
		}

		logger.exiting(URLParser.class.getName(), "uricNoSlash", result);

		return result;
	}

	protected String uricString() throws ParseException {
		StringBuilder retval = new StringBuilder();

		while(true) {
			String next = uric();

			if(next == null) {
				char la = lexer.lookAhead(0);

				// allow IPv6 addresses in generic URI strings e.g. http://[::1]
				if(la == '[') {
					HostNameParser hnp = new HostNameParser(this.getLexer());
					HostPort hp = hnp.hostPort(false);

					retval.append(hp.toString());

					continue;
				}

				break;
			}

			retval.append(next);
		}

		return retval.toString();
	}

	/**
	 * Parse and return a structure for a generic URL. Note that non SIP URLs are
	 * just stored as a string (not parsed).
	 * 
	 * @return URI is a URL structure for a SIP URL.
	 * @throws ParseException if there was a problem parsing.
	 */
	public GenericURI uriReference(boolean inBrackets) throws ParseException {
		logger.entering(URLParser.class.getName(), "uriReference");

		GenericURI retval = null;
		Token[] tokens = lexer.peekNextToken(2);
		Token t1 = tokens[0];
		Token t2 = tokens[1];

		if(t1.getTokenType() == TokenTypes.SIP || t1.getTokenType() == TokenTypes.SIPS) {
			if(t2.getTokenType() == ':') {
				retval = sipURL(inBrackets);
			} else {
				throw createParseException("Expecting \':\'");
			}
		} else if(t1.getTokenType() == TokenTypes.TEL) {
			if(t2.getTokenType() == ':') {
				retval = telURL(inBrackets);
			} else {
				throw createParseException("Expecting \':\'");
			}
		} else {
			String urlString = uricString();

			try {
				retval = new GenericURI(urlString);
			} catch (ParseException ex) {
				throw createParseException(ex.getMessage());
			}
		}

		logger.exiting(URLParser.class.getName(), "uriReference", retval);

		return retval;
	}

	/**
	 * Parser for the base phone number.
	 */
	private String basePhoneNumber() throws ParseException {
		logger.entering(URLParser.class.getName(), "basePhoneNumber");

		StringBuilder s = new StringBuilder();

		int lc = 0;
		while(lexer.hasMoreChars()) {
			char w = lexer.lookAhead(0);

			if(StringTokenizer.isDigit(w) || w == '-' || w == '.' || w == '(' || w == ')') {
				lexer.consume(1);
				s.append(w);
				lc++;
			} else if (lc > 0) {
				break;
			} else {
				throw createParseException("unexpected " + w);
			}
		}

		logger.exiting(URLParser.class.getName(), "basePhoneNumber", s);

		return s.toString();
	}

	/**
	 * Parser for the local phone #.
	 */
	private String localNumber() throws ParseException {
		logger.entering(URLParser.class.getName(), "localNumber");

		StringBuilder s = new StringBuilder();

		int lc = 0;
		while(lexer.hasMoreChars()) {
			char la = lexer.lookAhead(0);

			// allow 'A'..'F', should be upper case
			if(la == '*' || la == '#' || la == '-' || la == '.' || la == '(' || la == ')'
					|| StringTokenizer.isHexDigit(la)) {
				lexer.consume(1);
				s.append(la);
				lc++;
			} else if (lc > 0)
				break;
			else
				throw createParseException("unexepcted " + la);
		}

		logger.exiting(URLParser.class.getName(), "localNumber", s);

		return s.toString();
	}

	/**
	 * Parser for telephone subscriber.
	 *
	 * @return the parsed telephone number.
	 */
	public final TelephoneNumber parseTelephoneNumber(boolean inBrackets) throws ParseException {
		logger.entering(URLParser.class.getName(), "parseTelephoneNumber");

		TelephoneNumber tn;

		lexer.selectLexer(CHAR_LEXER);
		char c = lexer.lookAhead(0);

		if(c == '+') {
			tn = globalPhoneNumber(inBrackets);
		} else if(StringTokenizer.isHexDigit(c) || c == '#' || c == '*' || c == '-' || c == '.' || c == '('
				|| c == ')') {
			// see RFC3966
			tn = localPhoneNumber(inBrackets);
		} else {
			throw createParseException("unexpected char " + c);
		}

		logger.exiting(URLParser.class.getName(), "parseTelephoneNumber", tn);

		return tn;
	}

	private final TelephoneNumber globalPhoneNumber(boolean inBrackets) throws ParseException {
		logger.entering(URLParser.class.getName(), "globalPhoneNumber", inBrackets);

		TelephoneNumber tn = new TelephoneNumber();

		tn.setGlobal(true);

		this.lexer.match(PLUS);

		String b = basePhoneNumber();

		tn.setPhoneNumber(b);

		if(lexer.hasMoreChars()) {
			char tok = lexer.lookAhead(0);
			if(tok == ';' && inBrackets) {
				this.lexer.consume(1);

				tn.setParameters(telParameters());
			}
		}

		logger.exiting(URLParser.class.getName(), "globalPhoneNumber", tn);

		return tn;
	}

	private TelephoneNumber localPhoneNumber(boolean inBrackets) throws ParseException {
		logger.entering(URLParser.class.getName(), "localPhoneNumber", inBrackets);

		TelephoneNumber tn = new TelephoneNumber();

		tn.setGlobal(false);

		String b = localNumber();

		tn.setPhoneNumber(b);

		if(lexer.hasMoreChars()) {
			Token tok = this.lexer.peekNextToken();

			if(SEMICOLON == tok.getTokenType() && inBrackets) {
				this.lexer.consume(1);

				tn.setParameters(telParameters());
			}
		}

		logger.exiting(URLParser.class.getName(), "localPhoneNumber", tn);

		return tn;
	}

	private NameValueList telParameters() throws ParseException {
		NameValueList nvList = new NameValueList();

		/* Need to handle 'phone-context' specially 'isub' (or 'ext') MUST appear first,
		 * but we accept any order here */

		while(true) {
			String pname = paramNameOrValue();

			// Handle 'phone-context' specially, it may start with '+'
			if(pname.equalsIgnoreCase("phone-context")) {
				nvList.set(phoneContext());
			} else {
				if(lexer.lookAhead(0) == '=') {
					lexer.consume(1);

					nvList.set(new NameValue(pname, paramNameOrValue(), false));
				} else {
					// flag param
					nvList.set(new NameValue(pname, "", true));
				}
			}

			if(lexer.lookAhead(0) == ';') {
				lexer.consume(1);
			} else {
				return nvList;
			}
		}
	}

	/**
	 * Parses the 'phone-context' parameter in tel: URLs
	 * 
	 * @throws ParseException
	 */
	private NameValue phoneContext() throws ParseException {
		lexer.match('=');

		char la = lexer.lookAhead(0);

		Object value;
		if(la == '+') {// global-number-digits
			lexer.consume(1);// skip '+'
			value = "+" + basePhoneNumber();
		} else if(StringTokenizer.isAlphaDigit(la)) {
			// more broad than allowed
			Token t = lexer.match(LexerCore.ID);

			value = t.getTokenValue();
		} else {
			throw new ParseException("Invalid phone-context:" + la, -1);
		}

		return new NameValue("phone-context", value, false);
	}

	/**
	 * Parse and return a structure for a Tel URL.
	 * 
	 * @return a parsed tel url structure.
	 */
	public TelURLImpl telURL(boolean inBrackets) throws ParseException {
		lexer.match(TokenTypes.TEL);
		lexer.match(':');

		TelephoneNumber tn = this.parseTelephoneNumber(inBrackets);
		TelURLImpl telUrl = new TelURLImpl();

		telUrl.setTelephoneNumber(tn);

		return telUrl;
	}

	/**
	 * Parse and return a structure for a SIP URL.
	 * 
	 * @return a URL structure for a SIP URL.
	 * @throws ParseException if there was a problem parsing.
	 */
	public SipUri sipURL(boolean inBrackets) throws ParseException {
		logger.entering(URLParser.class.getName(), "sipURL");

		SipUri retval = new SipUri();
		Token nextToken = lexer.peekNextToken();
		int sipOrSips = TokenTypes.SIP;
		String scheme = TokenNames.SIP;

		if(nextToken.getTokenType() == TokenTypes.SIPS) {
			sipOrSips = TokenTypes.SIPS;
			scheme = TokenNames.SIPS;
		}

		lexer.match(sipOrSips);
		lexer.match(':');

		retval.setScheme(scheme);

		int startOfUser = lexer.markInputPosition();

		// Note: user may contain ';', host may not...
		String userOrHost = user();
		String passOrPort = null;

		// name:password or host:port
		if (lexer.lookAhead() == ':') {
			lexer.consume(1);
			passOrPort = password();
		}

		// name@hostPort
		if (lexer.lookAhead() == '@') {
			lexer.consume(1);
			retval.setUser(userOrHost);
			if (passOrPort != null)
				retval.setUserPassword(passOrPort);
		} else {
			// then userOrHost was a host, backtrack just in case a ';' was eaten...
			lexer.rewindInputPosition(startOfUser);
		}

		HostNameParser hnp = new HostNameParser(this.getLexer());
		HostPort hp = hnp.hostPort(false);

		retval.setHostPort(hp);

		lexer.selectLexer(CHAR_LEXER);
		while (lexer.hasMoreChars()) {
			// If the URI is not enclosed in brackets, parameters belong to header
			if (lexer.lookAhead(0) != ';' || !inBrackets)
				break;
			lexer.consume(1);
			NameValue parms = uriParam();
			if (parms != null)
				retval.setUriParameter(parms);
		}

		if (lexer.hasMoreChars() && lexer.lookAhead(0) == '?') {
			lexer.consume(1);
			while (lexer.hasMoreChars()) {
				NameValue parms = qheader();
				retval.setQHeader(parms);
				if (lexer.hasMoreChars() && lexer.lookAhead(0) != '&')
					break;
				else
					lexer.consume(1);
			}
		}

		logger.exiting(URLParser.class.getName(), "sipURL", retval);

		return retval;
	}

	public String peekScheme() throws ParseException {
		return lexer.getString(':');
	}

	/**
	 * Get a name value for a given query header (ie one that comes after the ?).
	 */
	protected NameValue qheader() throws ParseException {
		String name = lexer.getNextToken('=');
		lexer.consume(1);
		String value = hvalue();
		return new NameValue(name, value, false);

	}

	protected String hvalue() throws ParseException {
		StringBuilder retval = new StringBuilder();
		while (lexer.hasMoreChars()) {
			char la = lexer.lookAhead(0);
			// Look for a character that can terminate a URL.
			boolean isValidChar = false;
			switch (la) {
			case '+':
			case '?':
			case ':':
			case '[':
			case ']':
			case '/':
			case '$':
			case '_':
			case '-':
			case '"':
			case '!':
			case '~':
			case '*':
			case '.':
			case '(':
			case ')':
				isValidChar = true;
				break;
			default:
				isValidChar = false;
				break;
			}

			if(isValidChar || StringTokenizer.isAlphaDigit(la)) {
				lexer.consume(1);
				retval.append(la);
			} else if(la == '%') {
				retval.append(escaped());
			} else {
				break;
			}
		}

		return retval.toString();
	}

	/**
	 * Scan forward until you hit a terminating character for a URL. We do not
	 * handle non sip URLs in this implementation.
	 * 
	 * @return the string that takes us to the end of this URL (i.e. to the next
	 *         delimiter).
	 */
	protected String urlString() {
		StringBuilder retval = new StringBuilder();

		lexer.selectLexer(CHAR_LEXER);

		while(lexer.hasMoreChars()) {
			char la = lexer.lookAhead(0);

			// Look for a character that can terminate a URL.
			if(la == ' ' || la == '\t' || la == '\n' || la == '>' || la == '<') {
				break;
			}

			lexer.consume(0);

			retval.append(la);
		}

		return retval.toString();
	}

	protected String user() {
		logger.entering(URLParser.class.getName(), "user");

		int startIdx = lexer.getPtr();
		while (lexer.hasMoreChars()) {
			char la = lexer.lookAhead(0);
			if (isUnreserved(la) || isUserUnreserved(la)) {
				lexer.consume(1);
			} else if (isEscaped()) {
				lexer.consume(3);
			} else
				break;
		}

		String result = lexer.getBuffer().substring(startIdx, lexer.getPtr());

		logger.exiting(URLParser.class.getName(), "user", result);

		return result;
	}

	protected String password() {
		int startIdx = lexer.getPtr();
		while(true) {
			char la = lexer.lookAhead(0);

			boolean isValidChar;
			switch(la) {
			case '&':
			case '=':
			case '+':
			case '$':
			case ',':
				isValidChar = true;
				break;
			default:
				isValidChar = false;
				break;
			}

			if(isValidChar || isUnreserved(la)) {
				lexer.consume(1);
			} else if(isEscaped()) {
				lexer.consume(3);
			} else {
				break;
			}
		}

		return lexer.getBuffer().substring(startIdx, lexer.getPtr());
	}

	/**
	 * Default parse method. This method just calls uriReference.
	 */
	public GenericURI parse() throws ParseException {
		return uriReference(true);
	}
}
