/**
 *
 * @author pquiring
 */

import java.util.*;
import javaforce.SQL;

import javaforce.webui.*;
import javaforce.controls.*;
import javaforce.*;

public class ReportsPanel extends Panel {
  public ReportsPanel() {
    Calendar c = Calendar.getInstance();
    int year = c.get(Calendar.YEAR);
    int month = c.get(Calendar.MONTH) + 1;
    int day = c.get(Calendar.DAY_OF_MONTH);
    int hh = c.get(Calendar.HOUR_OF_DAY);
    int mm = c.get(Calendar.MINUTE);
    int ss = c.get(Calendar.SECOND);
    String date = String.format("%04d-%02d-%02d", year, month, day);
    String time = String.format("%02d:%02d:%02d", hh, mm, ss);

    Row row = new Row();
    add(row);

    back = new Button("Back");
    back.addClickListener((m, comp) -> {
      comp.getClient().setPanel(new MainPanel());
    });
    row.add(back);

    row.add(new Label("Tag"));
    tags = new ComboBox();
    tags.setWidth("200px");
    row.add(tags);

    row.add(new Label("Start Date"));
    date_start = new TextField(date);
    row.add(date_start);

    row.add(new Label("Start Time"));
    time_start = new TextField("00:00:00");
    row.add(time_start);

    row.add(new Label("End Date"));
    date_end = new TextField(date);
    row.add(date_end);

    row.add(new Label("End Time"));
    time_end = new TextField(time);
    row.add(time_end);

    execute = new Button("View Report");
    execute.addClickListener((m, comp) -> {
      update();
    });
    row.add(execute);

    report = new TextArea("");
    report.addClass("pad");
    add(report);

    loadTags();
  }
  public ComboBox tags;
  public TextField date_start, time_start;
  public TextField date_end, time_end;
  public Button back, execute;
  public TextArea report;

  public Tag list[];

  private String getTag(String id) {
    for(int a=0;a<list.length;a++) {
      String tid = (String)list[a].getData("id");
      if (tid.equals(id)) {
        return list[a].toString();
      }
    }
    return "?tag.id=" + id;
  }

  private void update() {
    int idx = tags.getSelectedIndex();
    if (idx == -1) {
      JFLog.log("no tag selected for report");
      return;
    }
    String query;
    String start = date_start.getText() + " " + time_start.getText();
    String end = date_end.getText() + " " + time_end.getText();
    String name;
    if (idx == 0) {
      //all tags
      query = "select id,value,when from history where when >= " + SQL.quote(start) + " and when <=" + SQL.quote(end);
      name = "All tags";
    } else {
      Tag tag = list[idx-1];
      query = "select id,value,when from history where id=" + tag.getData("id") + " and when >= " + SQL.quote(start) + " and when <=" + SQL.quote(end);
      name = tag.tag;
    }
    String data[][] = Service.queryHistory(query);
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<data.length;a++) {
      for(int b=0;b<data[a].length;b++) {
        if (b > 0) sb.append(",");
        if (b == 0) {
          sb.append(getTag(data[a][b]));  //convert id to tag name
        } else {
          sb.append(data[a][b]);
        }
      }
      sb.append("\r\n");
    }
    sb.append("End of report:" + name);
    report.setText(sb.toString());
  }
  public void loadTags() {
    list = Service.getTags();
    tags.clear();
    tags.add("*", "All tags");
    for(int a=0;a<list.length;a++) {
      String url = list[a].toString();
      tags.add("i" + a, url);
    }
  }
}
