package javax.sip.header;

import javax.sip.InvalidArgumentException;
import java.util.Locale;

/**
 * The Accept-Language header field is used in requests to indicate the
 * preferred languages for reason phrases, session descriptions, or status
 * responses carried as message bodies in the response. If no Accept-Language
 * header field is present, the server SHOULD assume all languages are
 * acceptable to the client. The q-value is used in a similar manner to
 * AcceptHeader to indicate degrees of preference.
 * <p>
 * For Example:<br>
 * <code>Accept-Language: da, en-gb;q=0.8, en;q=0.7</code>
 *
 * @see AcceptHeader
 */
public interface AcceptLanguageHeader extends Parameters, Header {
	/**
	 * Gets q-value of the media-range in AcceptLanguageHeader. A value of
	 * <code>-1</code> indicates the<code>q-value</code> is not set.
	 *
	 * @return q-value of media-range, -1 if q-value is not set.
	 */
	public float getQValue();

	/**
	 * Sets q-value for media-range in AcceptLanguageHeader. Q-values allow the user
	 * to indicate the relative degree of preference for that media-range, using the
	 * qvalue scale from 0 to 1. If no q-value is present, the media-range should be
	 * treated as having a q-value of 1.
	 *
	 * @param qValue - the new float value of the q-value, a value of -1 resets the
	 *               qValue.
	 * @throws InvalidArgumentException if the q parameter value is not
	 *                                  <code>-1</code> or between
	 *                                  <code>0 and 1</code>.
	 */
	public void setQValue(float qValue) throws InvalidArgumentException;

	/**
	 * Gets the language value of the AcceptLanguageHeader.
	 *
	 * @return the language Locale value of this AcceptLanguageHeader
	 */
	public Locale getAcceptLanguage();

	/**
	 * Sets the language parameter of this AcceptLanguageHeader.
	 * 
	 * @param language - the new Locale value of the language of
	 * 
	 *                 AcceptLanguageHeader
	 */
	public void setAcceptLanguage(Locale language);

	/**
	 * Name of AcceptLanguageHeader
	 */
	public static final String NAME = "Accept-Language";
}
