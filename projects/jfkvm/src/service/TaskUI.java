package service;

/** TaskUI
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

public class TaskUI extends Row {

  public TaskUI(Task task) {
    Calendar cal = Calendar.getInstance();
    setBorder(true);
    action = new Label(task.action);
    action.setWidth(150);
    action.setBorder(true);
    add(action);
    cal.setTimeInMillis(task.ts_start);
    start = new Label(JF.Time2String(cal));
    start.setWidth(100);
    start.setBorder(true);
    add(start);
    end = new Label("--:--:--");
    end.setWidth(100);
    end.setBorder(true);
    add(end);
    delta = new Label("--:--:--");
    delta.setWidth(100);
    delta.setBorder(true);
    add(delta);
    result = new Label("...");
    result.setWidth(100);
    result.setBorder(true);
    add(result);

    task.tasks.add(this);
  }

  public void update(Task task) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(task.ts_stop);
    end.setText(JF.Time2String(cal));
    delta.setText(Integer.toString((int)(task.ts_delta / 1000L)) + "s");
    result.setText(task.result);
  }

  public Label action;
  public Label start;
  public Label end;
  public Label delta;
  public Label result;
}
