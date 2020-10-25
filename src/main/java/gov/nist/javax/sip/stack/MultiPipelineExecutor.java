package gov.nist.javax.sip.stack;

import gov.nist.core.NamingThreadFactory;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiPipelineExecutor<K> {
	private static final Logger logger = Logger.getLogger(MultiPipelineExecutor.class.getName());

	private ExecutorService executor;
	private ConcurrentHashMap<K, SemaphoreLinkedList<SemaphoreRunnable<K>>> map = new ConcurrentHashMap<>();

	public MultiPipelineExecutor(int threads) {
		executor = Executors.newFixedThreadPool(threads, new NamingThreadFactory("jain_sip_multi_pipeline_executor"));
	}

	public synchronized void addTask(K key, Runnable task) {
		SemaphoreLinkedList<SemaphoreRunnable<K>> list = map.get(key);
		if(list == null) {
			list = new SemaphoreLinkedList<>();

			map.put(key, list);
		}

		list.addFirst(new SemaphoreRunnable<>(task, list.semaphore, this));
		this.notifyAll();
	}
	
	public synchronized void processTasks() {
		try {
			this.wait();
		} catch(InterruptedException e1) {
			logger.log(Level.SEVERE, e1.getMessage(), e1);
		}

		Enumeration<K> keys = map.keys();
		K key;
		while (keys.hasMoreElements()) {
			key = keys.nextElement();
			SemaphoreLinkedList<SemaphoreRunnable<K>> e = map.get(key);
			if(!e.isEmpty() && e.semaphore.tryAcquire()){
				Runnable task = e.pollLast();
				executor.execute(task);
			}
			//if(e.isEmpty()) map.remove(key); //CME, fix it
		}
	}
	
	public void remove(K key) {
		map.remove(key);
	}
	
	public static class SemaphoreLinkedList<A> extends LinkedList<A> {
		private static final long serialVersionUID = 1L;
		Semaphore semaphore = new Semaphore(1);
	}
	
	public static class SemaphoreRunnable<K> implements Runnable {

		protected Runnable wrappedTask;
		protected Semaphore semaphore;
		protected MultiPipelineExecutor<K> parent;
		public SemaphoreRunnable(Runnable task, Semaphore semaphore, MultiPipelineExecutor<K> parent) {
			this.wrappedTask = task;
			this.semaphore = semaphore;
			this.parent = parent;
		}
		
		public void run() {
			try {
				wrappedTask.run();
			} finally {
				semaphore.release();
				parent.notifyAll();
			}
		}
		
	}
}
