package gov.nist.javax.sip.stack;

import java.io.Serializable;

/**
 * Routing algorithms return a list of hops to which the request is routed.
 */
public final class HopImpl extends Object implements javax.sip.address.Hop, Serializable {
	private static final long serialVersionUID = 1L;

	protected String host;
	protected int port;
	protected String transport;

	protected boolean defaultRoute; // This is generated from the proxy addr
	protected boolean uriRoute; // This is extracted from the requestURI.

	/**
	 * Debugging println.
	 */
	@Override
	public String toString() {
		return host + ":" + port + "/" + transport;
	}

	/**
	 * Create new hop given host, port and transport.
	 * 
	 * @param hostName   hostname
	 * @param portNumber port
	 * @param trans      transport
	 */
	public HopImpl(String hostName, int portNumber, String trans) {
		host = hostName;

		// for correct management of IPv6 addresses.
		if(host.indexOf(":") >= 0 && host.indexOf("[") < 0) {
			host = "[" + host + "]";
		}

		port = portNumber;
		transport = trans;
	}

	/**
	 * Creates new Hop
	 * 
	 * @param hop is a hop string in the form of host:port/Transport
	 * @throws IllegalArgument exception if string is not properly formatted or null.
	 */
	HopImpl(String hop) {
		if(hop == null) {
			throw new IllegalArgumentException("Null arg!");
		}

		int brack = hop.indexOf(']');
		int colon = hop.indexOf(':', brack);
		int slash = hop.indexOf('/', colon);

		if (colon > 0) {
			this.host = hop.substring(0, colon);
			String portstr;
			if (slash > 0) {
				portstr = hop.substring(colon + 1, slash);
				this.transport = hop.substring(slash + 1);
			} else {
				portstr = hop.substring(colon + 1);
				this.transport = "UDP";
			}
			try {
				port = Integer.parseInt(portstr);
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Bad port spec");
			}
		} else {
			if (slash > 0) {
				this.host = hop.substring(0, slash);
				this.transport = hop.substring(slash + 1);
				this.port = transport.equalsIgnoreCase("TLS") ? 5061 : 5060;
			} else {
				this.host = hop;
				this.transport = "UDP";
				this.port = 5060;
			}
		}

		// Validate it
		if (host == null || host.length() == 0)
			throw new IllegalArgumentException("no host!");

		// normalize
		this.host = this.host.trim();
		this.transport = this.transport.trim();

		if ((brack > 0) && host.charAt(0) != '[') {
			throw new IllegalArgumentException("Bad IPv6 reference spec");
		}
	}

	/**
	 * Returns the host string.
	 * 
	 * @return host String
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * Returns the port.
	 * 
	 * @return port integer.
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * returns the transport string.
	 */
	@Override
	public String getTransport() {
		return transport;
	}

	/**
	 * Return true if this is uriRoute
	 */
	public boolean isURIRoute() {
		return uriRoute;
	}

	/**
	 * Set the URIRoute flag.
	 */
	public void setURIRouteFlag() {
		uriRoute = true;
	}
}
