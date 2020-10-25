package gov.nist.javax.sip;

import java.io.IOException;

import javax.sip.ListeningPoint;
import javax.sip.header.ContactHeader;
import javax.sip.header.ViaHeader;

public interface ListeningPointExt extends ListeningPoint {
	/**
	 * WebSocket Transport constant: WS
	 */
	public static final String WS = "WS";

	/**
	 * WebSocket secure Transport constant: WSS
	 */
	public static final String WSS = "WSS";

	/**
	 * Create a contact for this listening point.
	 *
	 * @return a contact header corresponding to this listening point.
	 */
	ContactHeader createContactHeader();

	/**
	 * Send a heart beat to the specified IP address and port via this listening
	 * point. This method can be used to send out a period CR-LF for NAT keep alive.
	 */
	public void sendHeartbeat(String ipAddress, int port) throws IOException;

	/**
	 * Create a Via header for this listening point.
	 * 
	 * @return a via header corresponding to this listening point. Branch ID is set to NULL.
	 */
	public ViaHeader createViaHeader();
}
