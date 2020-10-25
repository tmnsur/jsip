package gov.nist.core;

import java.util.*;
import java.text.ParseException;

/**
 * Base string token splitter.
 */
public class StringTokenizer {
	// moving to char array instead of string, more efficient CPU and mem wise
	protected char[] buffer;
	protected int bufferLen;
	protected int ptr;
	protected int savedPtr;

	public StringTokenizer() {
		// nothing
	}

	public StringTokenizer(String buffer) {
		this.buffer = buffer.toCharArray();
		bufferLen = buffer.length();
		ptr = 0;
	}

	public String nextToken() {
		int startIdx = ptr;

		while (ptr < bufferLen) {
			char c = buffer[ptr];
			ptr++;
			if(c == '\n') {
				break;
			}
		}

		return String.valueOf(buffer, startIdx, ptr - startIdx);
	}

	public boolean hasMoreChars() {
		return ptr < bufferLen;
	}

	public static boolean isHexDigit(char ch) {
		return (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f') || isDigit(ch);
	}

	public static boolean isAlpha(char ch) {
		if(ch <= 127) {
			return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
		}

		return Character.isLowerCase(ch) || Character.isUpperCase(ch);
	}

	public static boolean isDigit(char ch) {
		if(ch <= 127) {
			return (ch <= '9' && ch >= '0');
		}

		return Character.isDigit(ch);
	}

	public static boolean isAlphaDigit(char ch) {
		if(ch <= 127) {
			return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch <= '9' && ch >= '0');
		}

		return Character.isLowerCase(ch) || Character.isUpperCase(ch) || Character.isDigit(ch);
	}

	public String getLine() {
		int startIdx = ptr;

		while (ptr < bufferLen && buffer[ptr] != '\n') {
			ptr++;
		}

		if(ptr < bufferLen && buffer[ptr] == '\n') {
			ptr++;
		}

		return String.valueOf(buffer,startIdx, ptr - startIdx);
	}

	public String peekLine() {
		int curPos = ptr;
		String retval = this.getLine();

		ptr = curPos;

		return retval;
	}

	public char lookAhead() {
		return lookAhead(0);
	}

	public char lookAhead(int k) {
		int index = ptr + k;

		return index < buffer.length ? buffer[index] : '\0';
	}

	public char getNextChar() throws ParseException {
		if(ptr >= bufferLen) {
			throw new ParseException(new String(buffer) + " getNextChar: End of buffer", ptr);
		}

		return buffer[ptr++];
	}

	public void consume() {
		ptr = savedPtr;
	}

	public void consume(int k) {
		ptr += k;
	}

	/**
	 * Get a Vector of the buffer tokenized by lines
	 */
	public Vector<String> getLines() {
		Vector<String> result = new Vector<>();

		while(hasMoreChars()) {
			String line = getLine();

			result.addElement(line);
		}

		return result;
	}

	/**
	 * Get the next token from the buffer.
	 */
	public String getNextToken(char delim) throws ParseException {
		int startIdx = ptr;
		while(true) {
			char la = lookAhead(0);
			if(la == delim) {
				break;
			}

			if(la == '\0') {
				throw new ParseException("EOL reached", 0);
			}

			consume(1);
		}

		return String.valueOf(buffer, startIdx, ptr - startIdx);
	}

	/**
	 * get the SDP field name of the line
	 * 
	 * @return String
	 */
	public static String getSDPFieldName(String line) {
		if(line == null) {
			return null;
		}

		String fieldName = null;
		try {
			int begin = line.indexOf(Separators.EQUAL);

			fieldName = line.substring(0, begin);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}

		return fieldName;
	}
}
