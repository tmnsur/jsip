package gov.nist.javax.sip;

public interface UtilsExt {
	/**
	 * Generate a call identifier. This is useful when we want to generate a call
	 * identifier in advance of generating a message.
	 */
	public String generateCallIdentifier(String address);

	/**
	 * Generate a tag for a FROM header or TO header. Just return a random 4 digit
	 * integer (should be enough to avoid any clashes!) Tags only need to be unique
	 * within a call.
	 *
	 * @return a string that can be used as a tag parameter.
	 *
	 *         synchronized: needed for access to 'rand', else risk to generate same
	 *         tag twice
	 */
	public String generateTag();

	/**
	 * Generate a cryptographically random identifier that can be used to generate a
	 * branch identifier.
	 *
	 * @return a cryptographically random gloablly unique string that can be used as
	 *         a branch identifier.
	 */
	public String generateBranchId();
}
