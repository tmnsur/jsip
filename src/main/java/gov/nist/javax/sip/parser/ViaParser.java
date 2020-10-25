package gov.nist.javax.sip.parser;

import java.text.ParseException;
import java.util.logging.Logger;

import gov.nist.core.HostNameParser;
import gov.nist.core.HostPort;
import gov.nist.core.LexerCore;
import gov.nist.core.NameValue;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.Protocol;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;

/**
 * Parser for via headers.
 */
public class ViaParser extends HeaderParser {
	private static final Logger logger = Logger.getLogger(ViaParser.class.getName());

	public ViaParser(String via) {
		super(via);
	}

	public ViaParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * a parser for the essential part of the via header.
	 */
	private void parseVia(Via v) throws ParseException {
		// The protocol
		lexer.match(TokenTypes.ID);
		Token protocolName = lexer.getNextToken();

		this.lexer.SPorHT();
		// consume the "/"
		lexer.match('/');
		this.lexer.SPorHT();
		lexer.match(TokenTypes.ID);
		this.lexer.SPorHT();
		Token protocolVersion = lexer.getNextToken();

		this.lexer.SPorHT();

		// We consume the "/"
		lexer.match('/');
		this.lexer.SPorHT();
		lexer.match(TokenTypes.ID);
		this.lexer.SPorHT();

		Token transport = lexer.getNextToken();
		this.lexer.SPorHT();

		Protocol protocol = new Protocol();

		protocol.setProtocolName(protocolName.getTokenValue());
		protocol.setProtocolVersion(protocolVersion.getTokenValue());
		protocol.setTransport(transport.getTokenValue());

		v.setSentProtocol(protocol);

		// sent-By
		HostNameParser hnp = new HostNameParser(this.getLexer());
		HostPort hostPort = hnp.hostPort(true);

		v.setSentBy(hostPort);

		// Ignore blanks
		this.lexer.SPorHT();

		// parameters
		while (lexer.lookAhead(0) == ';') {
			this.lexer.consume(1);
			this.lexer.SPorHT();

			NameValue nameValue = this.nameValue();
			String name = nameValue.getName();

			if(name.equals(Via.BRANCH)) {
				String branchId = (String) nameValue.getValueAsObject();

				if(branchId == null) {
					throw new ParseException("null branch Id", lexer.getPtr());
				}
			}

			v.setParameter(nameValue);

			this.lexer.SPorHT();
		}

		// RFC3261 does not allow a comment in Via headers anymore
		if(lexer.lookAhead(0) == '(') {
			this.lexer.selectLexer("charLexer");

			lexer.consume(1);

			StringBuilder comment = new StringBuilder();
			while(true) {
				char ch = lexer.lookAhead(0);
				if(ch == ')') {
					lexer.consume(1);

					break;
				}

				if(ch == '\\') {
					// Escaped character
					Token tok = lexer.getNextToken();

					comment.append(tok.getTokenValue());

					lexer.consume(1);

					tok = lexer.getNextToken();
					comment.append(tok.getTokenValue());

					lexer.consume(1);
				} else if(ch == '\n') {
					break;
				} else {
					comment.append(ch);
					lexer.consume(1);
				}
			}

			v.setComment(comment.toString());
		}
	}

	/**
	 * Overrides the superclass nameValue parser because we have to tolerate IPV6
	 * addresses in the received parameter.
	 */
	@Override
	protected NameValue nameValue() throws ParseException {
		logger.entering(ViaParser.class.getName(), "nameValue");

		lexer.match(LexerCore.ID);

		Token name = lexer.getNextToken();

		// eat white space.
		lexer.SPorHT();

		NameValue nv;
		try {
			boolean quoted = false;

			char la = lexer.lookAhead(0);

			if(la == '=') {
				lexer.consume(1);
				lexer.SPorHT();

				String str = null;
				if(name.getTokenValue().compareToIgnoreCase(Via.RECEIVED) == 0) {
					// Allow for IPV6 Addresses.
					// these could have : in them!
					str = lexer.byteStringNoSemicolon();
				} else {
					if(lexer.lookAhead(0) == '\"') {
						str = lexer.quotedString();
						quoted = true;
					} else {
						lexer.match(LexerCore.ID);

						Token value = lexer.getNextToken();

						str = value.getTokenValue();
					}
				}

				nv = new NameValue(name.getTokenValue().toLowerCase(), str);

				if(quoted) {
					nv.setQuotedValue();
				}
			} else {
				nv = new NameValue(name.getTokenValue().toLowerCase(), null);
			}
		} catch(ParseException ex) {
			nv = new NameValue(name.getTokenValue(), null);
		}

		logger.exiting(ViaParser.class.getName(), "nameValue", nv);

		return nv;
	}

	@Override
	public SIPHeader parse() throws ParseException {
		logger.entering(ViaParser.class.getName(), "parse");

		ViaList viaList = new ViaList();

		// The first via header.
		this.lexer.match(TokenTypes.VIA);
		this.lexer.SPorHT(); // ignore blanks
		this.lexer.match(':'); // expect a colon.
		this.lexer.SPorHT(); // ignore blanks.

		while(true) {
			Via v = new Via();

			parseVia(v);

			viaList.add(v);

			this.lexer.SPorHT(); // eat whitespace.

			if(this.lexer.lookAhead(0) == ',') {
				this.lexer.consume(1); // Consume the comma
				this.lexer.SPorHT(); // Ignore space after.
			}

			if(this.lexer.lookAhead(0) == '\n') {
				break;
			}
		}

		this.lexer.match('\n');

		logger.exiting(ViaParser.class.getName(), "parse", viaList);

		return viaList;
	}
}
