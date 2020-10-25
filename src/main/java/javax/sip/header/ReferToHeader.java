package javax.sip.header;

/**
 * This interface represents the ReferTo SIP header, as defined by
 * <a href = "http://www.ietf.org/rfc/rfc3515.txt">RFC3515</a>, this header is
 * not part of RFC3261.
 * <p>
 * A ReferToHeader only appears in a REFER request. It provides a URL to
 * reference. The ReferToHeader field MAY be encrypted as part of end-to-end
 * encryption. The resource identified by the Refer-To URI is contacted using
 * the normal mechanisms for that URI type.
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public interface ReferToHeader extends HeaderAddress, Parameters, Header {
	/**
	 * Name of ReferToHeader
	 */
	public static final String NAME = "Refer-To";
}
