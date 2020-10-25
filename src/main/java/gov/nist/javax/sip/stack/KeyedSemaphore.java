package gov.nist.javax.sip.stack;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyedSemaphore {
	private static final Logger logger = Logger.getLogger(KeyedSemaphore.class.getName());

	ConcurrentHashMap<String, Semaphore> map = new ConcurrentHashMap<>();

	public void leaveIOCriticalSection(String key) {
		Semaphore creationSemaphore = map.get(key);

		if(creationSemaphore != null) {
			creationSemaphore.release();
		}
	}

	public void remove(String key) {
		if(map.get(key) != null) {
			map.get(key).release();
			map.remove(key);
		}
	}

	public void enterIOCriticalSection(String key) throws IOException {
		// http://dmy999.com/article/34/correct-use-of-concurrenthashmap
		Semaphore creationSemaphore = map.get(key);
		if (creationSemaphore == null) {
			Semaphore newCreationSemaphore = new Semaphore(1, true);
			creationSemaphore = map.putIfAbsent(key, newCreationSemaphore);
			if (creationSemaphore == null) {
				creationSemaphore = newCreationSemaphore;

				logger.log(Level.FINEST, "new Semaphore added for key {0}", key);
			}
		}

		try {
			boolean retval = creationSemaphore.tryAcquire(10, TimeUnit.SECONDS);
			if (!retval) {
				throw new IOException("Could not acquire IO Semaphore'" + key + "' after 10 seconds -- giving up ");
			}
		} catch (InterruptedException e) {
			throw new IOException("exception in acquiring sem");
		}
	}
}
