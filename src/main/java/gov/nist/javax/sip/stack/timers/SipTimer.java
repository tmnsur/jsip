package gov.nist.javax.sip.stack.timers;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.SIPStackTimerTask;

import java.util.Properties;

/**
 * Interface to implement to plug a new Timer implementation. currently the  ones provided with the stack are based
 * on java.util.Timer or java.util.concurrent.ScheduledThreadPoolExecutor
 */
public interface SipTimer {
	/**
	 * Schedule a new SIPStackTimerTask after the specified delay
	 * @param task the task to schedule
	 * @param delay the delay in milliseconds to schedule the task
	 * @return true if the task was correctly scheduled, false otherwise
	 */
	boolean schedule(SIPStackTimerTask task, long delay);
	
	/**
	 * Schedule a new SIPStackTimerTask after the specified delay
	 * @param task the task to schedule
	 * @param delay the delay in milliseconds to schedule the task
	 * @param period the period to run the task after it has been first scheduled 
	 * @return true if the task was correctly scheduled, false otherwise
	 */
	boolean scheduleWithFixedDelay(SIPStackTimerTask task, long delay, long period);
	
	/**
	 * Stop the Timer (called when the stack is stop or reinitialized)
	 */
	void stop();
	
	/**
	 * cancel a previously scheduled task
	 * @param task task to cancel
	 * @return true if the task was cancelled, false otherwise
	 */
	boolean cancel(SIPStackTimerTask task);
	
	/**
	 * Start the SIP Timer, called when the stack is created. The stack configuration is passed
	 * so that different implementations can use specific configuration properties to configure themselves
	 *
	 * @param sipStack
	 * @param configurationProperties the stack properties
	 */
	void start(SipStackImpl sipStack, Properties configurationProperties);

	/**
	 * Check if the timer is started or stopped
	 * @return true is the timer is started false otherwise
	 */
	boolean isStarted();
}
