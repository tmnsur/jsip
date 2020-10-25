package gov.nist.javax.sip.clientauthutils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * The class takes standard HTTP Authentication details and returns a response
 * according to the MD5 algorithm
 */
public class MessageDigestAlgorithm {
	private static final Logger logger = Logger.getLogger(MessageDigestAlgorithm.class.getName());

	private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Calculates an http authentication response in accordance with rfc2617.
	 * <p>
	 * 
	 * @param algorithm               a string indicating a pair of algorithms (MD5
	 *                                (default), or MD5-sess) used to produce the
	 *                                digest and a checksum.
	 * @param hashUserNameRealmPasswd MD5 hash of (username:realm:password)
	 * @param nonceValue             A server-specified data string provided in the
	 *                                challenge.
	 * @param cnonceValue            an optional client-chosen value whose purpose
	 *                                is to foil chosen plaintext attacks.
	 * @param method                  the SIP method of the request being
	 *                                challenged.
	 * @param digestUriValue        the value of the "uri" directive on the
	 *                                Authorization header in the request.
	 * @param entityBody             the entity-body
	 * @param qopValue               Indicates what "quality of protection" the
	 *                                client has applied to the message.
	 * @param ncValue                the hexadecimal count of the number of
	 *                                requests (including the current request) that
	 *                                the client has sent with the nonce value in
	 *                                this request.
	 * @return a digest response as defined in rfc2617
	 * @throws NullPointerException in case of incorrectly null parameters.
	 */
	static String calculateResponse(String algorithm, String hashUserNameRealmPasswd, String nonceValue, String ncValue,
			String cnonceValue, String method, String digestUriValue, String entityBody, String qopValue) {
		logger.entering(MessageDigestAlgorithm.class.getName(), "calculateResponse", new Object[] {
				algorithm,hashUserNameRealmPasswd, nonceValue, ncValue, cnonceValue, method, digestUriValue, entityBody,
				qopValue});

		if(hashUserNameRealmPasswd == null || method == null || digestUriValue == null || nonceValue == null) {
			throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");
		}

		// The following follows closely the algorithm for generating a response digest as specified by rfc2617
		if(cnonceValue == null || cnonceValue.length() == 0) {
			throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
		}

		String a2 = null;
		if(qopValue == null || qopValue.trim().length() == 0 || qopValue.trim().equalsIgnoreCase("auth")) {
			a2 = method + ":" + digestUriValue;
		} else {
			if(entityBody == null) {
				entityBody = "";
			}

			a2 = method + ":" + digestUriValue + ":" + h(entityBody);
		}

		String requestDigest = null;
		if(qopValue != null && ncValue != null
				&& (qopValue.equalsIgnoreCase("auth") || qopValue.equalsIgnoreCase("auth-int"))) {
			requestDigest = kd(hashUserNameRealmPasswd, nonceValue + ":" + ncValue + ":" + cnonceValue + ":"
				+ qopValue + ":" + h(a2));
		} else {
			requestDigest = kd(hashUserNameRealmPasswd, nonceValue + ":" + h(a2));
		}

		return requestDigest;
	}

	/**
	 * Calculates an HTTP authentication response in accordance with rfc2617.
	 * <p>
	 * 
	 * @param algorithm        a string indicating a pair of algorithms (MD5
	 *                         (default), or MD5-sess) used to produce the digest
	 *                         and a checksum.
	 * @param usernameValue   username_value (see rfc2617)
	 * @param realmValue      A string that has been displayed to the user in order
	 *                         to determine the context of the username and password
	 *                         to use.
	 * @param passwd           the password to encode in the challenge response.
	 * @param nonceValue      A server-specified data string provided in the
	 *                         challenge.
	 * @param cnonceValue     an optional client-chosen value whose purpose is to
	 *                         foil chosen plaintext attacks.
	 * @param method           the SIP method of the request being challenged.
	 * @param digestUriValue the value of the "uri" directive on the Authorization
	 *                         header in the request.
	 * @param entityBody      the entity-body
	 * @param qopValue        Indicates what "quality of protection" the client has
	 *                         applied to the message.
	 * @param ncValue         the hexadecimal count of the number of requests
	 *                         (including the current request) that the client has
	 *                         sent with the nonce value in this request.
	 * @return a digest response as defined in rfc2617
	 * @throws NullPointerException in case of incorrectly null parameters.
	 */
	static String calculateResponse(String algorithm, String usernameValue, String realmValue, String passwd,
			String nonceValue, String ncValue, String cnonceValue, String method, String digestUriValue,
			String entityBody, String qopValue) {
		logger.entering(MessageDigestAlgorithm.class.getName(), "calculateResponse", new Object[] {algorithm,
				usernameValue, realmValue, (passwd != null && passwd.trim().length() > 0), nonceValue, ncValue,
				cnonceValue, method, digestUriValue, entityBody, qopValue});

		if (usernameValue == null || realmValue == null || passwd == null || method == null
				|| digestUriValue == null || nonceValue == null)
			throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");

		// The following follows closely the algorithm for generating a response
		// digest as specified by rfc2617
		String a1 = null;

		if(algorithm == null || algorithm.trim().length() == 0 || algorithm.trim().equalsIgnoreCase("MD5")) {
			a1 = usernameValue + ":" + realmValue + ":" + passwd;
		} else {
			if(cnonceValue == null || cnonceValue.length() == 0) {
				throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
			}

			a1 = h(usernameValue + ":" + realmValue + ":" + passwd) + ":" + nonceValue + ":" + cnonceValue;
		}

		String a2 = null;
		if(qopValue == null || qopValue.trim().length() == 0 || qopValue.trim().equalsIgnoreCase("auth")) {
			a2 = method + ":" + digestUriValue;
		} else {
			if(entityBody == null) {
				entityBody = "";
			}

			a2 = method + ":" + digestUriValue + ":" + h(entityBody);
		}

		String requestDigest = null;

		if (cnonceValue != null && qopValue != null && ncValue != null
				&& (qopValue.equalsIgnoreCase("auth") || qopValue.equalsIgnoreCase("auth-int"))) {
			requestDigest = kd(h(a1),
					nonceValue + ":" + ncValue + ":" + cnonceValue + ":" + qopValue + ":" + h(a2));
		} else {
			requestDigest = kd(h(a1), nonceValue + ":" + h(a2));
		}

		return requestDigest;
	}

	/**
	 * Defined in RFC2617 as H(data) = MD5(data);
	 * 
	 * @param data data
	 * @return MD5(data)
	 */
	private static String h(String data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");

			return toHexString(digest.digest(data.getBytes()));
		} catch(NoSuchAlgorithmException ex) {
			// shouldn't happen
			throw new IllegalStateException("Failed to instantiate an MD5 algorithm", ex);
		}
	}

	/**
	 * Defined in RFC2617 as KD(secret, data) = H(concat(secret, ":", data))
	 * 
	 * @param data   data
	 * @param secret secret
	 * @return H(concat(secret, ":", data));
	 */
	private static String kd(String secret, String data) {
		return h(secret + ":" + data);
	}

	/**
	 * Converts b[] to hex string.
	 * 
	 * @param b the byte array to convert
	 * @return a Hex representation of b.
	 */
	private static String toHexString(byte[] b) {
		int pos = 0;
		char[] c = new char[b.length * 2];
		for (int i = 0; i < b.length; i++) {
			c[pos++] = toHex[(b[i] >> 4) & 0x0F];
			c[pos++] = toHex[b[i] & 0x0f];
		}
		return new String(c);
	}
}
