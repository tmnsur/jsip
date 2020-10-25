package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.LogRecord;
import gov.nist.javax.sip.LogRecordFactory;

/**
 * The Default Message log factory. This can be replaced as a stack
 * configuration parameter.
 */
public class DefaultMessageLogFactory implements LogRecordFactory {
	public LogRecord createLogRecord(String message, String source, String destination, String timeStamp,
			boolean isSender, String firstLine, String tid, String callId, long tsHeaderValue) {
		return new MessageLog(message, source, destination, timeStamp, isSender, firstLine, tid, callId, tsHeaderValue);
	}

	@Override
	public LogRecord createLogRecord(String message, String source, String destination, long timeStamp,
			boolean isSender, String firstLine, String tid, String callId, long timestampVal) {
		return new MessageLog(message, source, destination, timeStamp, isSender, firstLine, tid, callId, timestampVal);
	}
}
