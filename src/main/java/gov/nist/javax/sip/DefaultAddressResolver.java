package gov.nist.javax.sip;

import javax.sip.address.Hop;

import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.stack.HopImpl;
import gov.nist.javax.sip.stack.MessageProcessor;

/**
 * This is the default implementation of the AddressResolver. The
 * AddressResolver is a NIST-SIP specific feature. The address resolover is
 * consulted to convert a Hop into a meaningful address. The default
 * implementation is a passthrough. It only gets involved in setting the default
 * port. However, you can register your own AddressResolver implementation Note
 * that The RI checks incoming via headers for resolving the sentBy field. If
 * you want to set it to some address that cannot be resolved you should
 * register an AddressResolver with the stack. This feature is also useful for
 * DNS SRV lookup which is not implemented by the RI at present.
 *
 * @see gov.nist.javax.sip.SipStackImpl#setAddressResolver(AddressResolver)
 */
public class DefaultAddressResolver implements AddressResolver {
	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.core.net.AddressResolver#resolveAddress(javax.sip.address.Hop)
	 */
	public Hop resolveAddress(Hop inputAddress) {
		if(inputAddress.getPort() != -1) {
			return inputAddress;
		}

		return new HopImpl(inputAddress.getHost(), MessageProcessor.getDefaultPort(inputAddress.getTransport()),
				inputAddress.getTransport());
	}
}
