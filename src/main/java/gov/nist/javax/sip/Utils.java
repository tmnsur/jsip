package gov.nist.javax.sip;

import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPResponse;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * A few utilities that are used in various places by the stack. This is used to
 * convert byte arrays to hex strings etc. Generate tags and branch identifiers
 * and odds and ends.
 */
public class Utils implements UtilsExt {
	private static int digesterPoolsSize = 20;
	private static MessageDigest[] digesterPool = new MessageDigest[digesterPoolsSize];
	private static java.util.Random rand;
	private static long counter = 0;
	private static int callIDCounter;
	private static String signature;
	private static Utils instance = new Utils();

	/**
	 * to hex converter
	 */
	private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	static {
		try {
			for(int q = 0; q < digesterPoolsSize; q++) {
				digesterPool[q] = MessageDigest.getInstance("MD5");
			}
		} catch(Exception ex) {
			throw new IllegalStateException("Could not intialize Digester", ex);
		}

		rand = new java.util.Random(System.nanoTime());
		signature = toHexString(Integer.toString(Math.abs(rand.nextInt() % 1000)).getBytes());
	}

	public static Utils getInstance() {
		return instance;
	}

	/**
	 * convert an array of bytes to an hexadecimal string
	 *
	 * @return a string
	 * @param b bytes array to convert to a hexadecimal string
	 */
	public static String toHexString(byte[] b) {
		int pos = 0;

		char[] c = new char[b.length * 2];

		for(int i = 0; i < b.length; i++) {
			c[pos++] = toHex[(b[i] >> 4) & 0x0F];
			c[pos++] = toHex[b[i] & 0x0f];
		}

		return new String(c);
	}

	/**
	 * Put quotes around a string and return it. Any " characters appearing in str
	 * are escaped
	 *
	 * @return a quoted string
	 * @param str string to be quoted
	 */
	public static String getQuotedString(String str) {
		return '"' + str.replace("\"", "\\\"") + '"';
	}

	/**
	 * Squeeze out all white space from a string and return the reduced string.
	 *
	 * @param input input string to squeeze.
	 * @return String a reduced string.
	 */
	protected static String reduceString(String input) {
		String newString = input.toLowerCase();
		int len = newString.length();

		StringBuilder retval = new StringBuilder();

		for(int i = 0; i < len; i++) {
			if(newString.charAt(i) == ' ' || newString.charAt(i) == '\t') {
				continue;
			}

			retval.append(newString.charAt(i));
		}

		return retval.toString();
	}

	/**
	 * Converts the characters in this String to upper case.
	 * 
	 * @param str the String to convert. Using English as default as header strings
	 *            are in English. Resolves a bug in parsing and upper case when
	 *            default locale is set to Turkish.
	 * @return the String, converted to upper case.
	 */
	public static String toUpperCase(String str) {
		return str.toUpperCase(Locale.ENGLISH);
	}

	/**
	 * Generate a call identifier. This is useful when we want to generate a call
	 * identifier in advance of generating a message.
	 */
	@Override
	public String generateCallIdentifier(String address) {
		long random = rand.nextLong();
		int hash = (int) Math.abs(random % digesterPoolsSize);
		MessageDigest md = digesterPool[hash];

		synchronized (md) {
			String date = Long.toString(System.nanoTime() + System.currentTimeMillis() + callIDCounter++ + random);
			byte[] cid = md.digest(date.getBytes());

			String cidString = Utils.toHexString(cid);
			return cidString + "@" + address;
		}
	}

	/**
	 * Generate a tag for a FROM header or TO header. Just return a random 4 digit
	 * integer (should be enough to avoid any clashes!) Tags only need to be unique
	 * within a call.
	 *
	 * @return a string that can be used as a tag parameter.
	 *
	 *         synchronized: needed for access to 'rand', else risk to generate same
	 *         tag twice
	 */
	public synchronized String generateTag() {

		return Integer.toHexString(rand.nextInt());

	}

	/**
	 * Generate a cryptographically random identifier that can be used to generate a
	 * branch identifier.
	 *
	 * @return a cryptographically random gloablly unique string that can be used as
	 *         a branch identifier.
	 */
	public String generateBranchId() {
		long num = rand.nextLong() + (counter++) + System.currentTimeMillis() + System.nanoTime();
		int hash = (int) Math.abs(num % digesterPoolsSize);

		MessageDigest digester = digesterPool[hash];

		synchronized(digester) {
			byte[] bid = digester.digest(Long.toString(num).getBytes());

			// prepend with a magic cookie to indicate we are bis09 compatible.
			return SIPConstants.BRANCH_MAGIC_COOKIE + "-" + signature + "-" + Utils.toHexString(bid);
		}
	}

	public boolean responseBelongsToUs(SIPResponse response) {
		Via topmostVia = response.getTopmostVia();
		String branch = topmostVia.getBranch();

		return branch != null && branch.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE + "-" + signature);
	}

	public static String getSignature() {
		return signature;
	}
}
