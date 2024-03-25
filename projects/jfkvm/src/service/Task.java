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
import javaforce.webui.*;

public class Task extends Thread {
  protected long ts_start;
  protected long ts_stop;
  protected long ts_delta;

  public String action;
  public String result;

  public Panel tasks;
  public TaskUI taskui;

  public Task(String action) {
    this.action = action;
  }

  public final void run() {
    try {
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
