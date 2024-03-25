package service;

/** Tasks.
 *
 * Tasks run in the background and their results are displayed to any logged in user.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

public class Tasks extends Thread {
  private Object lock = new Object();
  private ArrayList<Task> taskList = new ArrayList<>();
  private boolean active = true;

  public void cancel() {
    active = false;
  }

  private void addUI(Task task) {
    task.taskui = new TaskUI(task);
    task.tasks.add(task.taskui);
  }

  public void addTask(Panel ui_tasks, Task task) {
    task.tasks = ui_tasks;
    task.ts_start = System.currentTimeMillis();
    addUI(task);
    synchronized (lock) {
      taskList.add(task);
    }
    task.start();
  }

  private void updateUI(Task task) {
    task.taskui.update(task);
  }


  public void completed(Task task) {
    task.ts_stop = System.currentTimeMillis();
    task.ts_delta = task.ts_stop - task.ts_start;
    updateUI(task);
    JFLog.log("Task completed:" + task.action + ":result=" + task.result);
    synchronized (lock) {
      taskList.remove(task);
    }
  }

  public void run() {
    //TODO monitor tasks and update clients
    while (active) {
      JF.sleep(1000);
    }
  }
}
