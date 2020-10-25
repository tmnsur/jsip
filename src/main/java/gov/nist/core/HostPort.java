package gov.nist.core;

import java.net.InetAddress;

/**
* Holds the hostname:port.
*/
public final class HostPort extends GenericObject {
	private static final long serialVersionUID = -7103412227431884523L;

	// host / ipv4/ ipv6/
	/**
	 * host field
	 */
	protected Host host;

	/**
	 * port field
	 */
	protected int port;

	/**
	 * Default constructor
	 */
	public HostPort() {
		host = null;
		port = -1; // marker for not set.
	}

	/**
	 * Encode this hostport into its string representation.
	 * Note that this could be different from the string that has
	 * been parsed if something has been edited.
	 * @return String
	 */
	public String encode() {
		return encode(new StringBuilder()).toString();
	}

	@Override
	public StringBuilder encode(StringBuilder buffer) {
		host.encode(buffer);

		if(port != -1) {
			buffer.append(COLON).append(port);
		}

		return buffer;
	}

	/**
	 * Returns true if the two objects are equals, false otherwise.
	 * 
	 * @param other Object to set
	 * @return boolean
	 */
	@Override
	public boolean equals(Object other) {
		if(null == other) {
			return false;
		}

		if(getClass() != other.getClass()) {
			return false;
		}

		HostPort that = (HostPort) other;

		return port == that.port && host.equals(that.host);
	}

	/**
	 * Get the Host field
	 * 
	 * @return host field
	 */
	public Host getHost() {
		return host;
	}

	/**
	 * Get the port field
	 * 
	 * @return int
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns boolean value indicating if Header has port
	 * @return boolean value indicating if Header has port
	 */
	public boolean hasPort() {
		return port != -1;
	}

	/**
	 * Remove port.
	 */
	public void removePort() {
		port = -1;
	}

	/**
	 * Set the host member
	 * @param h Host to set
	 */
	public void setHost(Host h) {
		host = h;
	}

	/**
	 * Set the port member
	 * @param p int to set
	 */
	public void setPort(int p) {
		port = p;
	}

	/**
	 * Return the internet address corresponding to the host.
	 * 
	 * @throws java.net.UnkownHostException if host name cannot be resolved.
	 * @return the inet address for the host.
	 */
	public InetAddress getInetAddress() throws java.net.UnknownHostException {
		if(host == null) {
			return null;
		}

		return host.getInetAddress();
	}

	@Override
	public void merge(Object mergeObject) {
		super.merge (mergeObject);

		if(port == -1) {
			port = ((HostPort) mergeObject).port;
		}
	}

	@Override
	public Object clone() {
		HostPort retval = (HostPort) super.clone();

		if(this.host != null) {
			retval.host = (Host) this.host.clone();
		}

		return retval;
	}

	@Override
	public String toString() {
		return this.encode();
	}

	@Override
	public int hashCode() {
		return this.host.hashCode() + this.port;
	}
}
