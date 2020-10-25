package gov.nist.core;

/**
 * Match template for pattern matching.
*/

public interface Match {
	/**
	 * Return true if a match occurs for searchString.
	 * This is used for pattern matching in the find and replace and match
	 * functions of the sipheaders and sdpfields packages. We have avoided
	 * picking a specific regexp package to avoid code dependencies.
	 * Use a package such as the jakarta regexp package to implement this.
	 */
	public boolean match(String searchString);
}
