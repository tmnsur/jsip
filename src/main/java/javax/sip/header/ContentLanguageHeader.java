package javax.sip.header;

import java.util.Locale;

/**
 * The Content-Language header field is used to indicate the language of the
 * message body.
 * <p>
 * For Example:<br>
 * <code>Content-Language: fr</code>
 *
 * @see ContentDispositionHeader
 * @see ContentLengthHeader
 * @see ContentEncodingHeader
 * @see ContentTypeHeader
 */
public interface ContentLanguageHeader extends Header {
	/**
	 * Gets the language value of the ContentLanguageHeader.
	 *
	 * @return the Locale value of this ContentLanguageHeader
	 */
	public Locale getContentLanguage();

	/**
	 * Sets the language parameter of this ContentLanguageHeader.
	 *
	 * @param language - the new Locale value of the language of
	 *                 ContentLanguageHeader
	 */
	public void setContentLanguage(Locale language);

	/**
	 * Name of ContentLanguageHeader
	 */
	public static final String NAME = "Content-Language";
}
