package javax.sip.header;

import java.text.ParseException;

/**
 * This interface represents methods for manipulating OptionTags values for any
 * header that contains an OptionTag value. Option tags are unique identifiers
 * used to designate new options (extensions) in SIP. Note that these options
 * appear as parameters in those header fields in an option-tag = token form.
 * Option tags are defined in standards track RFCs. This is a change from past
 * practice, and is instituted to ensure continuing multi-vendor
 * interoperability.
 *
 * @see ProxyRequireHeader
 * @see RequireHeader
 * @see UnsupportedHeader
 * @see SupportedHeader
 */

public interface OptionTag {
	/**
	 * Sets the option tag value to the new supplied optionTag parameter.
	 *
	 * @param optionTag - the new string value of the option tag.
	 * @throws ParseException which signals that an error has been reached
	 *                        unexpectedly while parsing the optionTag value.
	 */
	public void setOptionTag(String optionTag) throws ParseException;

	/**
	 * Gets the option tag of this OptionTag class.
	 *
	 * @return the string that identifies the option tag value.
	 */
	public String getOptionTag();
}
