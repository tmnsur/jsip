package gov.nist.javax.sip.address;

import java.text.ParseException;

import javax.sip.address.AddressFactory;

/**
 * This interface is extension to {@link javax.sip.address.AddressFactory}. It
 * declares methods which may be useful to user.
 */
public interface AddressFactoryEx extends AddressFactory {
	/**
	 * Creates SipURI instance from passed string.
	 * 
	 * @param sipUri - uri encoded string, it has form of:
	 *               <sips|sip>:username@host[:port]. NOTE: in case of IPV6, host
	 *               must be enclosed within [].
	 * @throws ParseException if the URI string is malformed.
	 */
	public javax.sip.address.SipURI createSipURI(String sipUri) throws ParseException;
}
