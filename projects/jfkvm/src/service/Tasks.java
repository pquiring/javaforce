package service;

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
  private boolean active = true;

  public void cancel() {
    active = false;
  }

  private void addUI(Task task) {
    task.taskui = new TaskUI(task);
    task.tasks.add(0, task.taskui);  //add at top of panel
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
  }

  public void removeTask(Task task) {
    synchronized (lock) {
      taskList.remove(task);
    }
    task.tasks.remove(task.taskui);
  }

  private static final long ts_cut_time = 5 * 60 * 1000;

  public void run() {
    int secs60 = 60;
    HTTP.setTimeout(5000);
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
      secs60++;
      if (secs60 > 60) {
        if (!check_hosts) {
          new CheckHosts().start();
        }
        secs60 = 0;
      }
    }
  }

  public boolean check_hosts;

  public class CheckHosts extends Thread {
    //run every minute
    public void run() {
      check_hosts = true;
      try {
        Host[] hosts = Config.current.getHosts();
        for(Host host : hosts) {
          try {
            if (!host.checkValid()) throw new Exception("invalid");
            if (!host.getVersion()) throw new Exception("offline");
            host.online = true;
            if (host.hostname == null) {
              host.hostname = host.getHostname();
              Config.current.save();
            }
          } catch (Exception e) {
            JFLog.log(e);
            host.online = false;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      Config.current.validateHosts();
      check_hosts = false;
    }
  }
}
