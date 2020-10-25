package gov.nist.javax.sip.stack.timers;

import gov.nist.core.NamingThreadFactory;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.SIPStackTimerTask;

import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the SIP Timer based on
 * java.util.concurrent.ScheduledThreadPoolExecutor Seems to perform
 */
public class ScheduledExecutorSipTimer implements SipTimer {
	private static final Logger logger = Logger.getLogger(ScheduledExecutorSipTimer.class.getName());

	protected SipStackImpl sipStackImpl;
	ScheduledThreadPoolExecutor threadPoolExecutor;

	// Counts the number of cancelled tasks
	private volatile int numCancelled = 0;

	public ScheduledExecutorSipTimer() {
		threadPoolExecutor = new ScheduledThreadPoolExecutor(1, new NamingThreadFactory("jain_sip_timer_executor"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#stop()
	 */
	public void stop() {
		threadPoolExecutor.shutdown();

		logger.log(Level.INFO, "the sip stack timer {0} has been stopped", this.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.javax.sip.stack.timers.SipTimer#schedule(gov.nist.javax.sip.stack.
	 * SIPStackTimerTask, long)
	 */
	public boolean schedule(SIPStackTimerTask task, long delay) {
		if (threadPoolExecutor.isShutdown()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		ScheduledFuture<?> future = threadPoolExecutor.schedule(new ScheduledSipTimerTask(task), delay,
				TimeUnit.MILLISECONDS);
		task.setSipTimerTask(future);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.javax.sip.stack.timers.SipTimer#scheduleWithFixedDelay(gov.nist.
	 * javax.sip.stack.SIPStackTimerTask, long, long)
	 */
	public boolean scheduleWithFixedDelay(SIPStackTimerTask task, long delay, long period) {
		if (threadPoolExecutor.isShutdown()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		ScheduledFuture<?> future = threadPoolExecutor.scheduleWithFixedDelay(new ScheduledSipTimerTask(task), delay,
				period, TimeUnit.MILLISECONDS);
		task.setSipTimerTask(future);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#start(gov.nist.javax.sip.
	 * SipStackImpl, java.util.Properties)
	 */
	@Override
	public void start(SipStackImpl sipStack, Properties configurationProperties) {
		sipStackImpl = sipStack;
		threadPoolExecutor.prestartAllCoreThreads();

		logger.log(Level.INFO, "the sip stack timer {0} has been started", this.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.javax.sip.stack.timers.SipTimer#cancel(gov.nist.javax.sip.stack.
	 * SIPStackTimerTask)
	 */
	@Override
	public boolean cancel(SIPStackTimerTask task) {
		boolean cancelled = false;
		ScheduledFuture<?> sipTimerTask = (ScheduledFuture<?>) task.getSipTimerTask();
		if (sipTimerTask != null) {
			task.cleanUpBeforeCancel();
			task.setSipTimerTask(null);
			threadPoolExecutor.remove((Runnable) sipTimerTask);
			cancelled = sipTimerTask.cancel(false);
		}
		// Purge is expensive when called frequently, only call it every now and then.
		// We do not sync the numCancelled variable. We dont care about correctness of
		// the number, and we will still call purge rought once on every 100 cancels.
		numCancelled++;
		if (numCancelled % 50 == 0) {
			threadPoolExecutor.purge();
		}
		return cancelled;
	}

	private class ScheduledSipTimerTask implements Runnable {
		private SIPStackTimerTask task;

		public ScheduledSipTimerTask(SIPStackTimerTask task) {
			this.task = task;
		}

		@Override
		public void run() {
			try {
				// task can be null if it has been cancelled
				if(task != null) {
					task.runTask();
				}
			} catch(Exception e) {
				logger.log(Level.SEVERE, "SIP stack timer task failed due to exception", e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#isStarted()
	 */
	public boolean isStarted() {
		return threadPoolExecutor.isTerminated();
	}
}
