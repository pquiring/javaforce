package javaforce.webui.tasks;

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
  protected boolean running;

  public int percent;
  public boolean successful;

  public String action;
  public String result;

  public Panel tasks;
  public TaskUI taskui;

  public Task parent;

  /** Creates new task with action. */
  public Task(String action) {
    this.action = action;
  }

  /** Creates new task with action and parent task.
   * Execution will wait until parent is completed.
   */
  public Task(String action, Task parent) {
    this.action = action;
    this.parent = parent;
  }

  public final void run() {
    try {
      if (parent != null) {
        Tasks.tasks.wait(parent);
      }
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
