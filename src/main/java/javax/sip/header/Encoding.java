package javax.sip.header;

import java.text.ParseException;

/**
 * This interface represents encoding methods for any header that contains an
 * encoding value.
 *
 * @see AcceptEncodingHeader
 * @see ContentEncodingHeader
 */
public interface Encoding {
	/**
	 * Sets the encoding of an EncodingHeader.
	 *
	 * @param encoding - the new string value defining the encoding.
	 * @throws ParseException which signals that an error has been reached
	 *                        unexpectedly while parsing the encoding value.
	 */

	public void setEncoding(String encoding) throws ParseException;

	/**
	 * Gets the encoding of an EncodingHeader. Returns null if no encoding is
	 * defined in an EncodingHeader.
	 *
	 * @return the string value identifying the encoding
	 */
	public String getEncoding();
}
