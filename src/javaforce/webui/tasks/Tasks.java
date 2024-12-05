package javaforce.webui.tasks;

/** Tasks.
 *
 * Tasks run in the background and their results are displayed to any logged in user.
 *
 * TODO : broadcast to other users
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

public class Tasks extends Thread {
  private Object lock = new Object();
  private ArrayList<Task> taskList = new ArrayList<>();
  private ArrayList<Task> waiting = new ArrayList<>();
  private boolean active = true;
  private boolean sequential = false;

  public static Tasks tasks;

  public static void init() {
    if (tasks != null) return;
    tasks = new Tasks();
    tasks.start();
  }

  public void cancel() {
    active = false;
  }

  /** Perform tasks one at a time. */
  public void setSequential(boolean state) {
    sequential = state;
  }

  private void addUI(Task task) {
    task.taskui = new TaskUI(task);
    task.tasks.add(0, task.taskui);  //add at top of panel
  }

  private boolean busy() {
    if (waiting.size() > 0) return true;
    for(Task task : taskList) {
      if (task.running) return true;
    }
    return false;
  }

  public void addTask(Panel ui_tasks, Task task) {
    task.tasks = ui_tasks;
    task.ts_start = System.currentTimeMillis();
    task.result = "";
    addUI(task);
    synchronized (lock) {
      if (sequential) {
        if (busy()) {
          waiting.add(task);
        } else {
          task.running = true;
          taskList.add(task);
          task.start();
        }
      } else {
        task.running = true;
        taskList.add(task);
        task.start();
      }
    }
  }

  private void updateUI(Task task) {
    task.taskui.update(task);
  }

  public void completed(Task task) {
    task.ts_stop = System.currentTimeMillis();
    task.ts_delta = task.ts_stop - task.ts_start;
    synchronized (lock) {
      task.running = false;
      if (sequential) {
        if (waiting.size() > 0) {
          Task next = waiting.remove(0);
          next.running = true;
          taskList.add(next);
          next.start();
        }
      }
    }
    updateUI(task);
    JFLog.log("Task completed:" + task.action + ":result=" + task.result);
  }

  /** Waits until task is completed. */
  public void wait(Task task) {
    //TODO : create sync list
    while (task.ts_stop == 0) {
      JF.sleep(100);
    }
  }

  public void removeTask(Task task) {
    synchronized (lock) {
      taskList.remove(task);
    }
    task.tasks.remove(task.taskui);
  }

  private static final long ts_cut_time = 5 * 60 * 1000;

  public void run() {
    while (active) {
      JF.sleep(1000);
      long ts_now = System.currentTimeMillis();
      long ts_cut = ts_now - ts_cut_time;
      ArrayList<Task> remove = new ArrayList<>();
      synchronized (lock) {
        for(Task task : taskList) {
          if (task.ts_stop == 0) continue;
          if (task.ts_stop < ts_cut) {
            remove.add(task);
          }
        }
        for(Task task : remove) {
          removeTask(task);
        }
      }
    }
  }
}
