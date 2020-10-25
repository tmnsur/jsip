package javax.sip.header;

/**
 * The Reply-To header field contains a logical return URI that may be different
 * from the From header field. For example, the URI MAY be used to return missed
 * calls or unestablished sessions. If the user wished to remain anonymous, the
 * header field SHOULD either be omitted from the request or populated in such a
 * way that does not reveal any private information.
 * <p>
 * For Example:<br>
 * <code>Reply-To: Bob sip:bob@biloxi.com</code>
 *
 * @see HeaderAddress
 * @see Parameters
 */
public interface ReplyToHeader extends HeaderAddress, Parameters, Header {
	/**
	 * Name of ReplyToHeader
	 */
	public static final String NAME = "Reply-To";
}
