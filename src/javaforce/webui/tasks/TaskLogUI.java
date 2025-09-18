package javaforce.webui.tasks;

/** Task Log UI Panel
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.webui.*;

public class TaskLogUI extends Panel {
  private TaskLog log;
  private Panel rows;
  private int year;
  private int month;
  private Label period;

  public TaskLogUI(TaskLog log) {
    this.log = log;
    //add controls
    Row row;
    row = new Row();
    row.add(new Label("Tasks Log"));
    Button prev = new Button("<");
    row.add(prev);
    period = new Label("YYYY-MM");
    row.add(period);
    Button next = new Button(">");
    row.add(next);
    Button refresh = new Button("Refresh");
    row.add(refresh);
    this.add(row);
    //add panel for TaskUI rows
    rows = new Panel();
    this.add(rows);
    prev.addClickListener((e, c) -> {
      if (year == 0) return;
      month--;
      if (month == 0) {
        year--;
        month = 12;
      }
      reload();
    });
    next.addClickListener((e, c) -> {
      if (year == 0) return;
      month++;
      if (month == 13) {
        year++;
        month = 1;
      }
      reload();
    });
    refresh.addClickListener((e, c) -> {
      if (year == 0) return;
      reload();
    });
    Calendar now = Calendar.getInstance();
    year = now.get(Calendar.YEAR);
    month = now.get(Calendar.MONTH) + 1;
    reload();
  }
  private void reload() {
    period.setText(String.format("%d-%02d", year, month));
    TaskEvent[] events = log.getEvents(year, month);
    rows.removeAll();
    if (events.length == 0) {
      Row row = new Row();
      row.add(new Label("No tasks found"));
      rows.add(row);
    } else {
      addTasks(events);
    }
  }
  private void addTasks(TaskEvent[] events) {
    for(TaskEvent event : events) {
      rows.add(new TaskUI(event));
    }
  }
}
