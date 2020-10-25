package gov.nist.javax.sip.parser;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.message.SIPRequest;

/**
 * Parser for CSeq headers.
 */
public class CSeqParser extends HeaderParser {
	public CSeqParser(String cseq) {
		super(cseq);
	}

	protected CSeqParser(Lexer lexer) {
		super(lexer);
	}

	@Override
	public SIPHeader parse() throws ParseException {
		try {
			CSeq c = new CSeq();

			headerName(CSEQ);

			String number = this.lexer.number();

			c.setSeqNumber(Long.parseLong(number));

			this.lexer.SPorHT();

			String m = SIPRequest.getCannonicalName(method()).intern();

			c.setMethod(m);

			this.lexer.SPorHT();
			this.lexer.match('\n');

			return c;
		} catch(NumberFormatException | InvalidArgumentException ex) {
			throw createParseException("Number format exception");
		}
	}
}
