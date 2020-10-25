package gov.nist.javax.sip;

import gov.nist.javax.sip.header.*;

/**
 * Default constants for SIP.
 */
public interface SIPConstants
		extends SIPHeaderNames, gov.nist.javax.sip.address.ParameterNames, gov.nist.javax.sip.header.ParameterNames {
	public static final int DEFAULT_PORT = 5060;
	public static final int DEFAULT_TLS_PORT = 5061;

	/**
	 * Prefix for the branch parameter that identifies BIS 09 compatible branch
	 * strings. This indicates that the branch may be as a global identifier for
	 * identifying transactions.
	 */
	public static final String BRANCH_MAGIC_COOKIE = "z9hG4bK";
	public static final String BRANCH_MAGIC_COOKIE_LOWER_CASE = "z9hg4bk";
	public static final String BRANCH_MAGIC_COOKIE_UPPER_CASE = "Z9HG4BK";
	/**
	 * constant SIP_VERSION_STRING
	 */
	public static final String SIP_VERSION_STRING = "SIP/2.0";
}
