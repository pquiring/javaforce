package javaforce.webui.tasks;

/** TaskUI
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

public class TaskUI extends Row {

  public TaskUI(TaskEvent event) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(event.time_start);
    setBorder(true);

    action = new Label(event.action);
    action.setWidth(300);
    action.setBorder(true);
    add(action);

    user = new Label(event.user);
    user.setWidth(100);
    user.setBorder(true);
    add(user);

    ip = new Label(event.ip);
    ip.setWidth(100);
    ip.setBorder(true);
    add(ip);

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
    result.setWidth(300);
    result.setBorder(true);
    add(result);
  }

  public void update(TaskEvent event) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(event.time_complete);
    end.setText(JF.Time2String(cal));
    delta.setText(Integer.toString((int)(event.time_duration / 1000L)) + "s");
    result.setText(event.result);
  }

  public void updateMessage(TaskEvent event) {
    result.setText(event.result);
  }

  public Label action;
  public Label user;
  public Label ip;
  public Label start;
  public Label end;
  public Label delta;
  public Label result;
}
