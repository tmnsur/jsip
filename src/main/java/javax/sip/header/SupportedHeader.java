package javax.sip.header;

/**
 * The Supported header field enumerates all the extensions supported by the UAC
 * or UAS. The Supported header field contains a list of option tags, that are
 * understood by the UAC or UAS. A User Agent compliant to this specification
 * MUST only include option tags corresponding to standards-track RFCs. If
 * empty, it means that no extensions are supported.
 * <p>
 * For Example:<br>
 * <code>Supported: 100rel</code>
 *
 * @see OptionTag
 * @see UnsupportedHeader
 */
public interface SupportedHeader extends OptionTag, Header {
	/**
	 * Name of SupportedHeader
	 */
	public static final String NAME = "Supported";
}
