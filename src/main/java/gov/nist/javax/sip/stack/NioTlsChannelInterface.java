package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.ClientTransactionExt;
import gov.nist.javax.sip.SipStackImpl;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Common interface for TLS channels. We should be able to invoke some methods in generic way.
 */
public interface NioTlsChannelInterface {
	/**
	 * Be able to send already encrypted data or metadata or some SSL frame padding to comply with some extension
	 *
	 * @param msg
	 * @throws IOException
	 */
	void sendEncryptedData(byte[] msg) throws IOException;

	/**
	 * Initialize the buffer again.
	 *
	 * @return
	 */
	ByteBuffer prepareAppDataBuffer();

	/**
	 * Initialize the buffer again.
	 *
	 * @return
	 */
	ByteBuffer prepareAppDataBuffer(int capacity);

	/**
	 * Initialize the buffer again.
	 *
	 * @return
	 */
	ByteBuffer prepareEncryptedDataBuffer();

	/**
	 * Add plain text data in the queue. It will be encrypted later in generic way
	 *
	 * @param bytes
	 * @throws Exception
	 */
	void addPlaintextBytes(byte[] bytes) throws Exception;

	/**
	 * Returns the SIP Stack associated with this channel
	 *
	 * @return
	 */
	SipStackImpl getSIPStack();

	/**
	 * Returns the Client Transaction associated with this channel
	 *
	 * @return
	 */
	ClientTransactionExt getEncapsulatedClientTransaction();
}
