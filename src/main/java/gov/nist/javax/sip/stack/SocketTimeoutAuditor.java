package gov.nist.javax.sip.stack;

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketTimeoutAuditor extends SIPStackTimerTask {
	private static final Logger logger = Logger.getLogger(SocketTimeoutAuditor.class.getName());

	private long nioSocketMaxIdleTime;

	public SocketTimeoutAuditor(long nioSocketMaxIdleTime) {
		this.nioSocketMaxIdleTime = nioSocketMaxIdleTime;
	}

	@Override
	public void runTask() {
		try {
			// Reworked the method for https://java.net/jira/browse/JSIP-471
			logger.log(Level.FINEST, "keys to check for inactivity removal: {0}",
					NioTcpMessageChannel.channelMap.keySet());

			Iterator<Entry<SocketChannel, NioTcpMessageChannel>> entriesIterator = NioTcpMessageChannel.channelMap
					.entrySet().iterator();

			while(entriesIterator.hasNext()) {
				Entry<SocketChannel, NioTcpMessageChannel> entry = entriesIterator.next();
				SocketChannel socketChannel = entry.getKey();
				NioTcpMessageChannel messageChannel = entry.getValue();

				if(System.currentTimeMillis() - messageChannel.getLastActivityTimestamp() > nioSocketMaxIdleTime) {
					logger.log(Level.FINEST, "Will remove, socket: {0}, lastActivity: {1}, current: {2},"
							+ " socketChannel: {3}", new Object[] { messageChannel.key,
									messageChannel.getLastActivityTimestamp(), System.currentTimeMillis(),
											socketChannel});

					messageChannel.close();
					entriesIterator = NioTcpMessageChannel.channelMap.entrySet().iterator();
				} else {
					logger.log(Level.FINEST, "don't remove, socket: {0} as lastActivity: {1} and current: {2},"
							+ "socketChannel: {3}", new Object[] {messageChannel.key,
									messageChannel.getLastActivityTimestamp(), System.currentTimeMillis(),
											socketChannel});
				}
			}
		} catch(Exception e) {
			logger.log(Level.FINEST, "silently skipping exception", e);
		}
	}
}
