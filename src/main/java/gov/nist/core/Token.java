package gov.nist.core;

/**
 * Base token class.
 */
public class Token {
	protected String tokenValue;
	protected int tokenType;

	public String getTokenValue() {
		return this.tokenValue;
	}

	public int getTokenType() {
		return this.tokenType;
	}

	@Override
	public String toString() {
		return "tokenValue = " + tokenValue + "/tokenType = " + tokenType;
	}
}
