package javax.sip.header;

import java.text.ParseException;

/**
 * The Subject header field provides a summary or indicates the nature of the
 * call, allowing call filtering without having to parse the session
 * description. The session description does not have to use the same subject
 * indication as the invitation.
 * <p>
 * For Example:<br>
 * <code>Subject: Where is the Moscone?</code>
 *
 */

public interface SubjectHeader extends Header {
	/**
	 * Sets the subject value of the SubjectHeader to the supplied string
	 * subject value.
	 *
	 * @param subject - the new subject value of this header.
	 * @throws ParseException which signals that an error has been reached
	 *                        unexpectedly while parsing the subject value.
	 */
	public void setSubject(String subject) throws ParseException;

	/**
	 * Gets the subject value of SubjectHeader.
	 * 
	 * @return subject of SubjectHeader.
	 */
	public String getSubject();

	/**
	 * Name of SubjectHeader
	 */
	public static final String NAME = "Subject";
}
