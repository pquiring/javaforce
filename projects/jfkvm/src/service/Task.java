package service;

/** Task.
 *
 * Runs in the background.
 *
 * Tasks should extend and override doTask() method.
 *
 * @author pquiring
 */

import javaforce.*;

public class Task extends Thread {
  private long start;

  public String action;
  public String result;

  public Task(String action) {
    this.action = action;
  }

  public final void run() {
    try {
      start = System.currentTimeMillis();
      doTask();
    } catch (Exception e) {
      JFLog.log(e);
    }
    KVMService.tasks.completed(this);
  }

  /** Performs task in a thread. */
  public void doTask() {}

  public void setResult(String msg) {
    result = msg;
  }
}
