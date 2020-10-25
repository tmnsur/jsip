package gov.nist.javax.sip.parser;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.stack.SIPStackTimerTask;
import gov.nist.javax.sip.stack.timers.SipTimer;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Input class for the pipelined parser. Buffer all bytes read from the socket
 * and make them available to the message parser.
 */
public class Pipeline extends InputStream {
	private LinkedList buffList;
	private Buffer currentBuffer;
	private boolean isClosed;
	private SipTimer timer;
	private InputStream pipe;
	private int readTimeout;
	private SIPStackTimerTask myTimerTask;

	class MyTimer extends SIPStackTimerTask {
		Pipeline pipeline;

		private boolean isCancelled;

		protected MyTimer(Pipeline pipeline) {
			this.pipeline = pipeline;
		}

		public void runTask() {
			if (this.isCancelled) {
				this.pipeline = null;
				return;
			}

			try {
				pipeline.close();
			} catch (IOException ex) {
				InternalErrorHandler.handleException(ex);
			}
		}

		@Override
		public void cleanUpBeforeCancel() {
			this.isCancelled = true;
			this.pipeline = null;
			super.cleanUpBeforeCancel();
		}

	}

	class Buffer {
		byte[] bytes;

		int length;

		int ptr;

		public Buffer(byte[] bytes, int length) {
			ptr = 0;
			this.length = length;
			this.bytes = bytes;
		}

		public int getNextByte() {
			return (int) bytes[ptr++] & 0xFF;
		}

	}

	public void startTimer() {
		if (this.readTimeout == -1)
			return;
		// TODO make this a tunable number. For now 4 seconds
		// between reads seems reasonable upper limit.
		this.myTimerTask = new MyTimer(this);
		this.timer.schedule(this.myTimerTask, this.readTimeout);
	}

	public void stopTimer() {
		if (this.readTimeout == -1)
			return;
		if (this.myTimerTask != null)
			this.timer.cancel(myTimerTask);
	}

	public Pipeline(InputStream pipe, int readTimeout, SipTimer timer) {
		// pipe is the Socket stream
		// this is recorded here to implement a timeout.
		this.timer = timer;
		this.pipe = pipe;
		buffList = new LinkedList();
		this.readTimeout = readTimeout;
	}

	public void write(byte[] bytes, int start, int length) throws IOException {
		if (this.isClosed)
			throw new IOException("Closed!!");
		Buffer buff = new Buffer(bytes, length);
		buff.ptr = start;
		synchronized (this.buffList) {
			buffList.add(buff);
			buffList.notifyAll();
		}
	}

	public void write(byte[] bytes) throws IOException {
		if (this.isClosed)
			throw new IOException("Closed!!");
		Buffer buff = new Buffer(bytes, bytes.length);
		synchronized (this.buffList) {
			buffList.add(buff);
			buffList.notifyAll();
		}
	}

	public void close() throws IOException {
		this.isClosed = true;
		synchronized (this.buffList) {
			this.buffList.notifyAll();
		}

		// JvB: added
		this.pipe.close();
	}

	public int read() throws IOException {
		synchronized (this.buffList) {
			if(currentBuffer != null && currentBuffer.ptr < currentBuffer.length) {
				int retval = currentBuffer.getNextByte();

				if(currentBuffer.ptr == currentBuffer.length) {
					this.currentBuffer = null;
				}

				return retval;
			}

			if(this.isClosed && this.buffList.isEmpty()) {
				return -1;
			}

			try {
				// wait till something is posted.
				while (this.buffList.isEmpty()) {
					this.buffList.wait();

					// Issue 314 : return -1 only is the buffer is empty
					if (this.buffList.isEmpty() && this.isClosed) {
						return -1;
					}
				}

				currentBuffer = (Buffer) this.buffList.removeFirst();

				int retval = currentBuffer.getNextByte();

				if(currentBuffer.ptr == currentBuffer.length) {
					this.currentBuffer = null;
				}

				return retval;
			} catch (InterruptedException ex) {
				throw new IOException(ex.getMessage());
			} catch (NoSuchElementException ex) {
				throw new IOException(ex.getMessage());
			}
		}
	}

	public boolean isClosed() {
		return isClosed;
	}
}