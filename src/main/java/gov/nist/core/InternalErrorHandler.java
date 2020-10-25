package gov.nist.core;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
*  Handle Internal error failures and print a stack trace (for debugging).
*/
public class InternalErrorHandler {
	private static final Logger logger = Logger.getLogger(InternalErrorHandler.class.getName());

	/**
	 * Handle an unexpected exception.
	 */
	public static void handleException(Exception ex) {
		logger.log(Level.SEVERE, "Unexpected internal error", ex);

		throw new IllegalStateException("Unexpected internal error " + ex.getMessage(), ex);
	}

	/**
	 * Handle an unexpected condition (and print the error code).
	 */
	public static void handleException(String emsg) {
		logger.log(Level.SEVERE, "Unexpected INTERNAL ERROR !!: {0}", emsg);

		throw new IllegalStateException(emsg);
	}
}
