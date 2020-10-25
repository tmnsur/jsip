package gov.nist.javax.sip.stack.timers;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.SIPStackTimerTask;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default SIP Timer implementation based on java.util.Timer
 *
 */
public class DefaultSipTimer extends Timer implements SipTimer {
	private static final Logger logger = Logger.getLogger(DefaultSipTimer.class.getName());

	protected AtomicBoolean started = new AtomicBoolean(false);
	protected SipStackImpl sipStackImpl;

	private class DefaultTimerTask extends TimerTask {
		private SIPStackTimerTask task;

		public DefaultTimerTask(SIPStackTimerTask task) {
			this.task = task;
			task.setSipTimerTask(this);
		}

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

		@Override
		public boolean cancel() {
			if(task != null) {
				task.cleanUpBeforeCancel();
				task = null;
			}

			return super.cancel();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.javax.sip.stack.timers.SipTimer#schedule(gov.nist.javax.sip.stack.
	 * SIPStackTimerTask, long)
	 */
	public boolean schedule(SIPStackTimerTask task, long delay) {
		if (!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		super.schedule(new DefaultTimerTask(task), delay);
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
		if (!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		super.schedule(new DefaultTimerTask(task), delay, period);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.javax.sip.stack.timers.SipTimer#cancel(gov.nist.javax.sip.stack.
	 * SIPStackTimerTask)
	 */
	public boolean cancel(SIPStackTimerTask task) {
		return ((TimerTask) task.getSipTimerTask()).cancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#start(gov.nist.javax.sip.
	 * SipStackImpl, java.util.Properties)
	 */
	public void start(SipStackImpl sipStack, Properties configurationProperties) {
		sipStackImpl = sipStack;
		// don't need the properties so nothing to see here
		started.set(true);

		logger.log(Level.INFO, "the sip stack timer {0} has been started", this.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#stop()
	 */
	public void stop() {
		started.set(false);
		cancel();
		logger.log(Level.INFO, "the sip stack timer {0} has been stopped", this.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#isStarted()
	 */
	public boolean isStarted() {
		return started.get();
	}

}
