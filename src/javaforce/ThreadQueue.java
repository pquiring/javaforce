package javaforce;

/**
 * This queue will process Runnable objects with a Thread pool.
 *
 * Parameters:
 *
 *   minThreads = min threads to maintain (default 1)
 *   maxThreads = max threads to create as needed (default 16)
 *   idleSeconds = idle time a thread will run before it is freed (default 60)
 *
 * Ex:
 *   ThreadQueue queue = new ThreadQueue();
 *
 *   queue.add(new Runnable() { public void run(){ ... }});
 *     or
 *   queue.add( () -> { ... } );  //lambda expression (Java 8+)
 *
 *   queue.close();  //free threads
 *
 * @author pquiring
 */

import java.util.concurrent.*;

public class ThreadQueue {
  private ThreadPoolExecutor executor;
  private BlockingQueue<Runnable> queue;

  /** Create ThreadQueue with default settings.
   * minThreads = 1
   * maxThreads = 16
   * idleSeconds = 60
   */
  public ThreadQueue() {
    queue = new LinkedBlockingQueue<Runnable>();
    executor = new ThreadPoolExecutor(1, 16, 60, TimeUnit.SECONDS, queue);
  }

  /** Create ThreadQueue with default settings.
   * minThreads = 1
   * @param maxThreads = max threads
   * idleSeconds = 60
   */
  public ThreadQueue(int maxThreads) {
    queue = new LinkedBlockingQueue<Runnable>();
    executor = new ThreadPoolExecutor(1, maxThreads, 60, TimeUnit.SECONDS, queue);
  }

  /** Create ThreadQueue with default settings.
   * @param minThreads = min threads
   * @param maxThreads = max threads
   * idleSeconds = 60
   */
  public ThreadQueue(int minThreads, int maxThreads) {
    queue = new LinkedBlockingQueue<Runnable>();
    executor = new ThreadPoolExecutor(minThreads, maxThreads, 60, TimeUnit.SECONDS, queue);
  }

  /** Create ThreadQueue with default settings.
   * @param minThreads = min threads
   * @param maxThreads = max threads
   * @param idleSeconds = idle seconds
   */
  public ThreadQueue(int minThreads, int maxThreads, int idleSeconds) {
    queue = new LinkedBlockingQueue<Runnable>();
    executor = new ThreadPoolExecutor(minThreads, maxThreads, idleSeconds, TimeUnit.SECONDS, queue);
  }

  /** Closes the ThreadQueue (the queue is first emptied) */
  public void close() {
    executor.shutdown();
    executor = null;
    queue = null;
  }

  /** Adds a Runnable object to ThreadQueue. */
  public void add(Runnable task) {
    executor.execute(task);
  }
}
