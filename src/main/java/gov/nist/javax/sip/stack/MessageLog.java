package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.LogRecord;

/**
 * This class stores a message along with some other informations Used to log
 * messages.
 */
class MessageLog implements LogRecord {
	private String message;
	private String source;
	private String destination;
	private long timeStamp;
	private boolean isSender;
	private String firstLine;
	private String tid;
	private String callId;
	private long timeStampHeaderValue;

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.LogRecord#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof MessageLog)) {
			return false;
		}

		MessageLog otherLog = (MessageLog) other;

		return otherLog.message.equals(message) && otherLog.timeStamp == timeStamp;
	}

	/**
	 * Constructor
	 */
	public MessageLog(String message, String source, String destination, String timeStamp, boolean isSender,
			String firstLine, String tid, String callId, long timeStampHeaderValue) {
		if(message == null || message.equals("")) {
			throw new IllegalArgumentException("null msg");
		}

		this.message = message;
		this.source = source;
		this.destination = destination;

		try {
			long ts = Long.parseLong(timeStamp);

			if(ts < 0) {
				throw new IllegalArgumentException("Bad time stamp ");
			}

			this.timeStamp = ts;
		} catch(NumberFormatException ex) {
			throw new IllegalArgumentException("Bad number format " + timeStamp);
		}

		this.isSender = isSender;
		this.firstLine = firstLine;
		this.tid = tid;
		this.callId = callId;
		this.timeStampHeaderValue = timeStampHeaderValue;
	}

	public MessageLog(String message, String source, String destination, long timeStamp, boolean isSender,
			String firstLine, String tid, String callId, long timestampVal) {
		if(message == null || message.equals("")) {
			throw new IllegalArgumentException("null msg");
		}

		this.message = message;
		this.source = source;
		this.destination = destination;

		if(timeStamp < 0) {
			throw new IllegalArgumentException("negative ts");
		}

		this.timeStamp = timeStamp;
		this.isSender = isSender;
		this.firstLine = firstLine;
		this.tid = tid;
		this.callId = callId;
		this.timeStampHeaderValue = timestampVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.LogRecord#toString()
	 */
	@Override
	public String toString() {
		String log;

		log = "<message\nfrom=\"" + source + "\" \nto=\"" + destination + "\" \ntime=\"" + timeStamp + "\""
				+ (this.timeStampHeaderValue != 0 ? "\ntimeStamp = \"" + timeStampHeaderValue + "\"" : "")
				+ "\nisSender=\"" + isSender + "\" \ntransactionId=\"" + tid + "\" \ncallId=\"" + callId
				+ "\" \nfirstLine=\"" + firstLine.trim() + "\"" + " \n>\n";
		log += "<![CDATA[";
		log += message;
		log += "]]>\n";
		log += "</message>\n";

		return log;
	}
}
