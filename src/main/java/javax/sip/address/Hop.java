package javax.sip.address;

/**
 * The Hop interface defines a location a request can transit on the way to its
 * destination, i.e. a route. It defines the host, port and transport of the
 * location. This interface is used to identify locations in the {@link Router}
 * interface.
 *
 * @see Router
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public interface Hop {
	/**
	 * Returns the host part of this Hop.
	 *
	 * @return the string value of the host.
	 */
	public String getHost();

	/**
	 * Returns the port part of this Hop.
	 *
	 * @return the integer value of the port.
	 */
	public int getPort();

	/**
	 * Returns the transport part of this Hop.
	 *
	 * @return the string value of the transport.
	 */
	public String getTransport();

	/**
	 * This method returns the Hop as a string.
	 *
	 * @return the stringified version of the Hop
	 */
	public String toString();
}
