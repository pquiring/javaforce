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
  protected boolean running;

  public int percent;

  public TaskEvent event;

  public Panel tasks;
  public TaskUI taskui;

  public Task parent;

  /** Creates new task with action, user, ip. */
  public Task(String action, String user, String ip) {
    event = new TaskEvent(action, user, ip);
  }

  /** Creates new task with TaskEvent. */
  public Task(TaskEvent event) {
    this.event = event;
  }

  /** Creates new task with action and parent task.
   * Execution will wait until parent is completed.
   */
  public Task(String action, String user, String ip, Task parent) {
    event = new TaskEvent(action, user, ip);
    this.parent = parent;
  }

  /** Creates new task with TaskEvent and parent task.
   * Execution will wait until parent is completed.
   */
  public Task(TaskEvent event, Task parent) {
    this.event = event;
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
    if (event.result == null) {
      //setResult() never called : assume failure
      event.result = "Error";
      event.successful = false;
    }
    Tasks.tasks.completed(this);
  }

  /** Performs task in a thread. */
  public void doTask() {}

  /** Set progress status update. */
  public void setStatus(String msg) {
    JFLog.log("Task:" + event.action + ":" + msg);
    event.result = msg;
    if (taskui != null) {
      taskui.updateMessage(event);
    }
  }

  public void setPercent(int value) {
    percent = value;
  }

  /** Set final task status completion. */
  public void setResult(String msg, boolean success) {
    percent = 100;
    event.successful = success;
    setStatus(msg);
  }

  public void setResult(boolean success) {
    percent = 100;
    event.successful = success;
  }

  public String getAction() {
    return event.action;
  }

  public String getResult() {
    return event.result;
  }

  public boolean getSuccessful() {
    return event.successful;
  }

  /** Create standard error message. */
  public String getError() {
    return "Error:" + event.action + " failed, check logs.";
  }
}
