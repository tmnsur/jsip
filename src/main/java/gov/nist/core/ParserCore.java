package gov.nist.core;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic parser class.
 * All parsers inherit this class.
 */
public abstract class ParserCore {
	private static final Logger logger = Logger.getLogger(ParserCore.class.getName());

	static AtomicInteger nestingLevel =new AtomicInteger();

	protected LexerCore lexer;

	protected NameValue nameValue(char separator) throws ParseException {
		if(logger.isLoggable(Level.FINEST)) {
			debugEnter("nameValue");
		}

		try {
			lexer.match(LexerCore.ID);
			Token name = lexer.getNextToken();

			// eat white space.
			lexer.SPorHT();
			try {
				boolean quoted = false;
				char la = lexer.lookAhead(0);

				if(la == separator) {
					lexer.consume(1);
					lexer.SPorHT();

					String str = null;
					boolean isFlag = false;
					if(lexer.lookAhead(0) == '\"') {
						str = lexer.quotedString();
						quoted = true;
					} else {
						lexer.match(LexerCore.ID);

						Token value = lexer.getNextToken();
						str = value.tokenValue;

						if(null == str) {
							str = "";
							isFlag = true;
						}
					}

					NameValue nv = new NameValue(name.tokenValue,str,isFlag);

					if(quoted) {
						nv.setQuotedValue();
					}

					return nv;
				}

				return new NameValue(name.tokenValue,"",true);
			} catch(ParseException ex) {
				return new NameValue(name.tokenValue,null,false);
			}
		} finally {
			if(logger.isLoggable(Level.FINEST)) {
				debugLeave("nameValue");
			}
		}
	}

	protected void debugEnter(String rule) {
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < nestingLevel.get() ; i++) {
			stringBuilder.append(">");
		}

		if(logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "{0}{1}\nlexer buffer = \n{2}",
					new Object[] {stringBuilder, rule, lexer.getRest()});
		}

		nestingLevel.incrementAndGet();
	}

	protected void debugLeave(String rule) {
		StringBuilder stringBuilder = new StringBuilder();

		for(int i = 0; i < nestingLevel.get() ; i++) {
			stringBuilder.append("<");
		}

		if(logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "{0}{1}\nlexer buffer = \n{2}",
					new Object[] {stringBuilder, rule, lexer.getRest()});
		}

		nestingLevel.decrementAndGet();
	}

	protected NameValue nameValue() throws ParseException  {
		return nameValue('=');
	}

	protected void peekLine(String rule) {
		if(logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "{0} {1}", new Object[] {rule, lexer.peekLine()});
		}
	}
}
