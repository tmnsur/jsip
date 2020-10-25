package gov.nist.javax.sip;

import javax.sip.IOExceptionEvent;

public class IOExceptionEventExt extends IOExceptionEvent {
	private static final long serialVersionUID = 1L;

	private final String myHost;
	private final int myPort;
	private Reason reason = null;

	public enum Reason {
		KeepAliveTimeout
	}

	public IOExceptionEventExt(Object source, Reason reason, String myHost, int myPort, String peerHost, int peerPort,
			String transport) {
		super(source, peerHost, peerPort, transport);
		this.myHost = myHost;
		this.myPort = myPort;
		this.reason = reason;
	}

	public String getLocalHost() {
		return myHost;
	}

	public int getLocalPort() {
		return myPort;
	}

	public String getPeerHost() {
		return getHost();
	}

	public int getPeerPort() {
		return getPort();
	}

	/**
	 * The reason for the Dialog Timeout Event being delivered to the application.
	 * 
	 * @return the reason for the timeout event.
	 */
	public Reason getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return "KeepAliveTimeoutEvent{" + "myHost='" + myHost + '\'' + ", myPort=" + myPort + ", peerHost='" + getHost()
				+ '\'' + ", peerPort=" + getPort() + ", transport='" + getTransport() + '\'' + '}';
	}
}
