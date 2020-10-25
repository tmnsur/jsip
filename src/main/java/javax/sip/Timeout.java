package javax.sip;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class contains the enumerations that define whether a timeout has
 * occurred in the underlying implementation. The application gets informed on
 * whether a retransmission or transaction timer has expired.
 *
 * There are two types of Timeout, namely:
 * <ul>
 * <li>{@link Timeout#RETRANSMIT} - This type is used to alert an application
 * that the underlying retransmit timer has expired so an application can
 * re-send the message specific to a transaction. This timer is defaulted to
 * 500ms and is doubled each time it is fired until the transaction expires
 * {@link Timeout#TRANSACTION}. The default retransmission value can be changed
 * per transaction using {@link Transaction#setRetransmitTimer(int)}. The
 * RETRANSMIT type is exposed to the following applications as follows:
 * <UL>
 * <li><b>User Agent</b> - Retransmissions on Invite transactions are the
 * responsibility of the application. This is due to the three way handshake for
 * an INVITE Request. All other retransmissions are handled by the underlying
 * implementation. Therefore the application will only receive this Timeout type
 * specific to Invite transactions.
 * <li><b>User Agent with No Retransmission</b> - an application can configure
 * an implementation to handle all retransmissions using property
 * characteristics of the {@link SipStack}. Therefore a Timeout of this type
 * will not be passed to the application. The application will only get notified
 * when the {@link Timeout#TRANSACTION} occurs.
 * <li><b>Stateful Proxy</b> - a stateful proxy remembers transaction state
 * about each incoming request and any requests it sends as a result of
 * processing the incoming request. The underlying implementation will handle
 * the retransmissions of all messages it sends and the application will not
 * receive {@link Timeout#RETRANSMIT} events, however the application will get
 * notified of {@link Timeout#TRANSACTION} events. As an Invite transaction is
 * end to end a stateful proxy will not handle the retransmissions of messages
 * on an Invite transaction, unless it decides to respond to the Invite
 * transaction, in essence becoming an User Agent Server and as such should
 * behave as described by the User Agent semantics above bearing in mind the
 * retranmission property of the underlying implementation.
 * <li><b>Stateless Proxy</b> - as a stateless proxy acts as a simple forwarding
 * agent, i.e. it simply forwards every message it receives upstream, it keeps
 * no transaction state for messages. The implementation does not retransmit
 * messages, therefore an application will not receive
 * {@link Timeout#RETRANSMIT} events on a message handled statelessly. If
 * retransmission semantics are required by an application using a stateless
 * method, it is the responsibility of the application to provide this feature,
 * not the underlying implementation.
 * </UL>
 * <li>{@link Timeout#TRANSACTION} - This type is used to alert an application
 * that the underlying transaction has expired. A transaction timeout typically
 * occurs at a time 64*T1 were T1 is the initial value of the
 * {@link Timeout#RETRANSMIT}, usually defaulted to 500ms. The TRANSACTION type
 * is exposed to the following applications as follows:
 * <UL>
 * <li><b>User Agent</b> - All retransmissions except retransmissions on Invite
 * transactions are the responsibility of the underlying implementation, i.e.
 * Invite transactions are the responsibility of the application. Therefore the
 * application will only recieve TRANSACTION Timeout events on transactions that
 * are not Invite transactions.
 * <li><b>User Agent with No Retransmission</b> - an application can configure
 * an implementation to handle all retransmissions using property
 * characteristics of the {@link SipStack}. Therefore a TRANSACTION Timeout will
 * be fired to the application on any transaction that expires including an
 * Invite transaction.
 * <li><b>Stateful Proxy</b> - a stateful proxy remembers transaction state
 * about each incoming request and any requests it sends as a result of
 * processing the incoming request. The underlying implementation handles the
 * retransmissions of all messages it sends and will notify the application of
 * {@link Timeout#TRANSACTION} events on any of its transactions. As an Invite
 * transaction is end to end a stateful proxy will not handle transaction
 * timeouts on an Invite transaction, unless it decides to respond to the Invite
 * transaction, in essence becoming an User Agent Server and as such should
 * behave as described by the User Agent semantics above bearing in mind the
 * retransmission property of the underlying implementation.
 * <li><b>Stateless Proxy</b> - as a stateless proxy acts as a simple forwarding
 * agent, i.e. it simply forwards every message it receives upstream, it keeps
 * no transaction state of the messages. The implementation does not maintain
 * transaction state, therefore an application will not receive
 * {@link Timeout#TRANSACTION} events on a message handled statelessly. If
 * transaction timeout semantics are required by an application using a
 * stateless method, it the responsibility of the application to provide this
 * feature, not the underlying implementation.
 * </ul>
 * </ul>
 *
 * @author BEA Systems, NIST
 * @version 1.2
 */
public final class Timeout implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final int SIZE = 2;
	private static final Timeout[] TIMEOUT_ARRAY = new Timeout[SIZE];

	/**
	 * This constant value indicates the internal value of the Retransmit timeout.
	 * <br>
	 * This constant has an integer value of 0.
	 */
	public static final int RETRANSMIT_VALUE = 0;
	/**
	 * This constant value indicates the "Retransmit" timeout.
	 */
	public static final Timeout RETRANSMIT = new Timeout(RETRANSMIT_VALUE);

	/**
	 * This constant value indicates the internal value of the Transaction timeout.
	 * <br>
	 * This constant has an integer value of 1.
	 */
	public static final int TRANSACTION_VALUE = 1;

	/**
	 * This constant value indicates the "Transaction" timeout.
	 */
	public static final Timeout TRANSACTION = new Timeout(TRANSACTION_VALUE);

	private int timeoutValue;

	private static void setTimeout(int timeoutValue, Timeout timeout) {
		TIMEOUT_ARRAY[timeoutValue] = timeout;
	}

	/**
	 * Constructor for the Timeout
	 *
	 * @param timeout the integer value for the Timeout
	 */
	private Timeout(int timeout) {
		this.timeoutValue = timeout;

		setTimeout(timeout, this);
	}

	/**
	 * This method returns the object value of the Timeout
	 *
	 * @return The Timeout Object
	 * @param timeout The integer value of the Timeout
	 */
	public Timeout getObject(int timeout) {
		if(timeout >= 0 && timeout < SIZE) {
			return TIMEOUT_ARRAY[timeout];
		}

		throw new IllegalArgumentException("Invalid timeout value");
	}

	/**
	 * This method returns the integer value of the Timeout
	 *
	 * @return The integer value of the Timeout
	 */
	public int getValue() {
		return timeoutValue;
	}

	/**
	 * Returns the designated type as an alternative object to be used when writing
	 * an object to a stream.
	 *
	 * This method would be used when for example serializing Timeout.RETRANSMIT and
	 * deserializing it afterwards results again in Timeout.RETRANSMIT. If you do
	 * not implement readResolve(), you would not get Timeout.RETRANSMIT but an
	 * instance with similar content.
	 *
	 * @return the Timeout
	 * @exception ObjectStreamException
	 */
	private Object readResolve() throws ObjectStreamException {
		return TIMEOUT_ARRAY[timeoutValue];
	}

	/**
	 * This method returns a string version of this class.
	 * 
	 * @return The string version of the Timeout
	 */
	public String toString() {
		String text = "";
		switch (timeoutValue) {
		case RETRANSMIT_VALUE:
			text = "Retransmission Timeout";
			break;

		case TRANSACTION_VALUE:
			text = "Transaction Timeout";
			break;
		default:
			text = "Error while printing Timeout";
			break;
		}
		return text;
	}
}
