package gov.nist.javax.sip.stack;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Issue http://java.net/jira/browse/JSIP-430 Allows to choose between direct vs non direct buffers
 */
public class ByteBufferFactory {
	private static final Logger logger = Logger.getLogger(ByteBufferFactory.class.getName());
	private static ByteBufferFactory instance = new ByteBufferFactory();
	private boolean useDirect = true;

	public static ByteBufferFactory getInstance() {
		return instance;
	}

	public ByteBuffer allocateDirect(int capacity) {
		logger.log(Level.FINEST, "Allocating direct buffer {0}", capacity);

		return useDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
	}

	public ByteBuffer allocate(int capacity) {
		logger.log(Level.FINEST, "Allocating buffer {0}", capacity);

		return ByteBuffer.allocate(capacity);
	}

	public void setUseDirect(boolean useDirect) {
		logger.log(Level.FINEST, "Direct buffers are {0}", (useDirect ? "enabled" : "disabled"));

		this.useDirect = useDirect;
	}
}
