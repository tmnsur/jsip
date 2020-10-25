package gov.nist.javax.sip.stack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Websocket handshake rev 13 and rev 8
 */
public class WebSocketHttpHandshake {
	private static final Logger logger = Logger.getLogger(WebSocketHttpHandshake.class.getName());

	private Map<String, String> headers = new HashMap<>();

	public byte[] createHttpResponse(String request) throws Exception {
		logger.log(Level.FINEST, "Request: {0}", request);

		InputStream is = new ByteArrayInputStream(request.getBytes());

		boolean isSecure = false;

		String line = readLine(is);

		if(line == null) {
			return null;
		}

		String[] parts = line.split(" ");
		if(parts.length >= 3) {
			headers.put("ctx", parts[1]);
		}

		while(!line.isEmpty()) {
			line = readLine(is);

			if(line == null) {
				continue;
			}

			if(line.isEmpty()) {
				continue;
			}

			parts = line.split(":", 2);
			if(parts.length != 2) {
				continue;
			}

			if(parts[0].toLowerCase().startsWith("sec-websocket-key")) {
				isSecure = true;
			}

			headers.put(parts[0].trim(), parts[1].trim());
		}

		if(isSecure) {
			is.read(new byte[8]);
		}

		//answer the handshake
		StringBuilder sb = new StringBuilder();
		String lineSeparator = "\r\n";

		sb.append("HTTP/1.1 101 Web Socket Protocol Handshake").append("\r\n");
		sb.append("Upgrade: WebSocket").append(lineSeparator);
		sb.append("Connection: Upgrade").append(lineSeparator);

		if(isSecure) {
			sb.append("Sec-");
		}

		sb.append("WebSocket-Origin: ").append(headers.get("Origin")).append(lineSeparator);

		if(isSecure) {
			sb.append("Sec-");
		}

		sb.append("WebSocket-Location: ws://").append(headers.get("Host")).

		append(headers.get("ctx")).append(lineSeparator);

		sb.append("Sec-WebSocket-Accept: ").append(computeRev13Response(headers.get("Sec-WebSocket-Key")))
				.append(lineSeparator);

		if(headers.get("Protocol") != null) {
			if(isSecure) {
				sb.append("Sec-");
			}

			sb.append("Protocol: ").append(headers.get("Protocol")).append(lineSeparator);
		}

		if(headers.get("Sec-WebSocket-Protocol") != null) {
			sb.append("Sec-WebSocket-Protocol: ").append(headers.get("Sec-WebSocket-Protocol")).append(lineSeparator);
		}

		sb.append(lineSeparator);

		String response = sb.toString();

		logger.log(Level.FINEST, "Response: {0}", response);

		return sb.toString().getBytes();
	}

	static String computeRev13Response(String key) {
		key = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA1");
			md.reset();

			return new String(Base64Coder.encode(md.digest(key.getBytes())));
		} catch(NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}


	private String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();

		int cr = '\r';
		int nl = '\n';

		boolean gotcr = false;

		while(true) {
			int input = is.read();

			if (input == -1) {
				return null;
			}

			if (input == cr) {
				gotcr = true;
				continue;
			}
			if (input == nl && gotcr) {
				break;
			} else if (input == nl) {
				//we do this only because the protocol
				//tells ut that there must be a cr before the nl.
				return null;
			}

			sb.append((char) input);
		}

		return sb.toString();
	}
}
