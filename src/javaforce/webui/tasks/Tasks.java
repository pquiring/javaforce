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
  private TaskLog log;

  public static Tasks tasks;

  /** Setup Tasks. */
  public static void init() {
    if (tasks != null) return;
    tasks = new Tasks();
    tasks.start();
  }

  /** Setup Tasks with TaskLog. */
  public static void init(String tasks_log_folder) {
    if (tasks != null) return;
    tasks = new Tasks();
    tasks.log = new TaskLog();
    tasks.log.setFolder(tasks_log_folder);
    tasks.start();
  }

  public void cancel() {
    active = false;
  }

  public static Tasks getTasks() {
    return tasks;
  }

  public TaskLog getTaskLog() {
    return log;
  }

  /** Perform tasks one at a time. */
  public void setSequential(boolean state) {
    sequential = state;
  }

  private void addUI(Task task) {
    task.taskui = new TaskUI(task.event);
    if (task.tasks != null) {
      task.tasks.add(0, task.taskui);  //add at top of panel
    }
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
    task.event.time_start = System.currentTimeMillis();
    task.event.result = "";
    addUI(task);
    if (log != null) {
      log.add(task.event);
    }
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
    task.taskui.update(task.event);
  }

  public void completed(Task task) {
    task.event.time_complete = System.currentTimeMillis();
    task.event.time_duration = task.event.time_complete - task.event.time_start;
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
    if (log != null) {
      log.complete(task.event);
    }
    updateUI(task);
    JFLog.log("Task completed:" + task.event.action + ":result=" + task.event.result);
  }

  /** Waits until task is completed. */
  public void wait(Task task) {
    //TODO : create sync list
    while (task.event.time_complete == 0) {
      JF.sleep(100);
    }
  }

  public void removeTask(Task task) {
    synchronized (lock) {
      taskList.remove(task);
    }
    if (task.tasks != null) {
      task.tasks.remove(task.taskui);
    }
  }

  private static final long time_cut_time = 5 * 60 * 1000;

  public void run() {
    while (active) {
      JF.sleep(1000);
      long time_now = System.currentTimeMillis();
      long time_cut = time_now - time_cut_time;
      ArrayList<Task> remove = new ArrayList<>();
      synchronized (lock) {
        for(Task task : taskList) {
          if (task.event.time_complete == 0) continue;
          if (task.event.time_complete < time_cut) {
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
