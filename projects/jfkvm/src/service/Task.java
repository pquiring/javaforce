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
import javaforce.webui.tasks.*;

public class Task extends Thread implements Status {
  protected long ts_start;
  protected long ts_stop;
  protected long ts_delta;

  public int percent;
  public boolean successful;

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
    Tasks.tasks.completed(this);
  }

  /** Performs task in a thread. */
  public void doTask() {}

  public void setResult(String msg) {
    result = msg;
  }

  public void setStatus(String msg) {
    result = msg;
  }

  public void setPercent(int value) {
    percent = value;
  }

  public void setResult(boolean result) {
    successful = result;
  }
}
