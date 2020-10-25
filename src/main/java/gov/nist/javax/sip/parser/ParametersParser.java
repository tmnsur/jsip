package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * parameters parser header.
 */
public abstract class ParametersParser extends HeaderParser {
	protected ParametersParser(Lexer lexer) {
		super((Lexer) lexer);
	}

	protected ParametersParser(String buffer) {
		super(buffer);
	}

	protected void parse(ParametersHeader parametersHeader) throws ParseException {
		this.lexer.SPorHT();
		while (lexer.lookAhead(0) == ';') {
			this.lexer.consume(1);
			// eat white space
			this.lexer.SPorHT();
			NameValue nv = nameValue();
			parametersHeader.setParameter(nv);
			// eat white space
			this.lexer.SPorHT();
		}
	}

	protected void parseNameValueList(ParametersHeader parametersHeader) throws ParseException {
		parametersHeader.removeParameters();
		while (true) {
			this.lexer.SPorHT();
			NameValue nv = nameValue();
			parametersHeader.setParameter(nv.getName(), (String) nv.getValueAsObject());
			// eat white space
			this.lexer.SPorHT();
			if (lexer.lookAhead(0) != ';')
				break;
			else
				lexer.consume(1);
		}
	}
}
