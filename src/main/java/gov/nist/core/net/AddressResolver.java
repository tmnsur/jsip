package gov.nist.core.net;

import javax.sip.address.Hop;

/**
 * An interface that allows you to customize address lookup.
 * The user can implement this interface to do DNS lookups or other lookup
 * schemes and register it with the stack.
 * The default implementation of the address resolver does nothing more than just return back
 * the Hop that it was passed (fixing up the port if necessary).
 * However, this behavior can be overridden. To override
 * implement this interface and register it with the stack using
 * {@link gov.nist.javax.sip.SipStackExt#setAddressResolver(AddressResolver)}.
 * This interface will be incorporated into version 2.0 of the JAIN-SIP Specification.
 */
public interface AddressResolver {
	/**
	 * Do a name lookup and resolve the given IP address.
	 * The default implementation is just an identity mapping
	 * (returns the argument).
	 *
	 * @param hop - an incoming Hop containing a potentially unresolved address.
	 * @return a new hop ( if the address is recomputed ) or the original hop
	 * if this is just an identity mapping ( the default behavior ).
	 */
	public Hop resolveAddress( Hop hop);
}
