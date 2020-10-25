package gov.nist.javax.sip.stack;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockingQueueDispatchAuditor extends TimerTask {
	private static final Logger logger = Logger.getLogger(BlockingQueueDispatchAuditor.class.getName());

	private Timer timer = null;
	private static int timerThreadCount = 0;
	private long totalReject = 0;
	private boolean started = false;
	private Queue<? extends Runnable> queue;
	private int timeout = 8000;

	public BlockingQueueDispatchAuditor(Queue<? extends Runnable> queue) {
		this.queue = queue;
	}

	public void start(int interval) {
		if (started)
			stop();
		started = true;
		timer = new Timer("BlockingQueueDispatchAuditor-Timer-" + timerThreadCount++, true);
		timer.scheduleAtFixedRate(this, interval, interval);
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void stop() {
		try {
			timer.cancel();
			timer = null;
		} catch (Exception e) {
			// not important
		} finally {
			started = false;
		}
	}

	public void run() {
		try {
			int removed = 0;
			synchronized(this.queue) {
				QueuedMessageDispatchBase runnable = (QueuedMessageDispatchBase) this.queue.peek();
				while(runnable != null) {
					QueuedMessageDispatchBase d = runnable;
					if (System.currentTimeMillis() - d.getReceptionTime() > timeout) {
						queue.poll();
						runnable = (QueuedMessageDispatchBase) this.queue.peek();
						removed++;
					} else {
						runnable = null;
					}
				}
			}

			if(removed > 0) {
				totalReject += removed;

				logger.log(Level.WARNING, "Removed stuck messages={0} total rejected={1} still in queue={2}",
						new Object[] {removed, totalReject, this.queue.size()});
			}
		} catch(Exception e) {
			logger.log(Level.WARNING, "Problem reaping old requests. This is not a fatal error.", e);
		}
	}
}
