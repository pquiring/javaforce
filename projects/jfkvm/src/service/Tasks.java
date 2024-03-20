package service;

/** Tasks.
 *
 * Tasks run in the background and their results are displayed to any logged in user.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class Tasks extends Thread {
  private Object lock = new Object();
  private ArrayList<Task> taskList = new ArrayList<>();
  private boolean active = true;

  public void cancel() {
    active = false;
  }

  public void addTask(Task task) {
    synchronized (lock) {
      taskList.add(task);
    }
    task.start();
  }

  public void completed(Task task) {

  }

  public void run() {
    //TODO monitor tasks and update clients
    while (active) {
      JF.sleep(1000);
    }
  }
}
