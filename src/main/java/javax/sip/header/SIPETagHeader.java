package javax.sip.header;

import java.text.ParseException;

/**
 * This interface represents the SIP-ETag header, as defined by
 * <a href = "http://www.ietf.org/rfc/rfc3903.txt">RFC3903</a>.
 * <p>
 * The SIP-ETag header is used by a server (event state collector) in a 2xx
 * response to PUBLISH in order to convey a unique entity tag for the published
 * state. The client may then use this tag in a
 * {@link javax.sip.header.SIPIfMatchHeader} to update previously published
 * state.
 * <p>
 * Sample syntax:<br>
 * <code>SIP-ETag: dx200xyz</code>
 *
 * <p>
 * A server must ignore Headers that it does not understand. A proxy must not
 * remove or modify Headers that it does not understand.
 */
public interface SIPETagHeader extends Header {
	/**
	 * Name of this header (no short form).
	 */
	public static final String NAME = "SIP-ETag";

	/**
	 * Returns the value of the entity-tag.
	 *
	 * @return the entity-tag
	 */
	public String getETag();

	/**
	 * Sets the entity-tag value of this header.
	 *
	 * @param etag the new value of the entity-tag
	 * @throws ParseException if the ETag syntax is invalid (not a valid token)
	 */
	public void setETag(String etag) throws ParseException;
}
