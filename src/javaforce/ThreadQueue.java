package javaforce;

/**
 * A special thread that will execute Runnable objects placed in queue.
 *
 * Ex:
 *   ThreadQueue queue = new ThreadQueue();
 *   queue.start();
 *
 *   queue.add(new Runnable() { public void run(){ ... }});
 *     or
 *   queue.add( () -> { ... } );  //lambda expression (Java 8+)
 *
 * @author pquiring
 */

import java.util.concurrent.*;

public class ThreadQueue extends Thread {
  private BlockingQueue<Runnable> queue;
  private boolean active = true;

  public ThreadQueue() {
    queue = new ArrayBlockingQueue<Runnable>(16);
  }

  public ThreadQueue(int capacity) {
    queue = new ArrayBlockingQueue<Runnable>(capacity);
  }

  public void run() {
    try {
      while (active) {
        Runnable r = queue.take();
        r.run();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Closes the ThreadQueue (the queue is first emptied) */
  public void close() {
    active = false;
    //wake up thread if waiting for next Runnable
    add(new Runnable() { public void run(){}});
  }

  /** Adds a Runnable object to ThreadQueue. */
  public void add(Runnable r) {
    queue.add(r);
  }
}
