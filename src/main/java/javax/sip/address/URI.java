package javax.sip.address;

import java.io.Serializable;

/**
 * This class represents a generic URI. This is the base interface for any type
 * of URI. These are used in SIP requests to identify the callee and also in
 * Contact, From, and To headers.
 * <p>
 * The generic syntax of URIs is defined in <a href =
 * http://www.ietf.org/rfc/rfc2396.txt>RFC 2396</a>.
 *
 * @see TelURL
 * @see SipURI
 */
public interface URI extends Cloneable, Serializable {
	/**
	 * Returns the value of the "scheme" of this URI, for example "sip", "sips" or
	 * "tel".
	 *
	 * @return the scheme parameter of the URI
	 */
	public String getScheme();

	/**
	 * Creates and returns a deep copy of the URI. This methods must ensure a deep
	 * copy of the URI, so that when a URI is cloned the URI can be modified without
	 * effecting the original URI. This provides useful functionality for proxying
	 * Requests and Responses. This method overrides the clone method in
	 * java.lang.Object.
	 *
	 * @return a deep copy of URI
	 */
	public Object clone();

	/**
	 * This method determines if this is a URI with a scheme of "sip" or "sips".
	 *
	 * @return true if the scheme is "sip" or "sips", false otherwise.
	 */
	public boolean isSipURI();

	/**
	 * This method returns the URI as a string.
	 *
	 * @return String The stringified version of the URI
	 */
	public String toString();
}
