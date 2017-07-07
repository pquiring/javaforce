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

    back = getButton("Back", 2);
    back.addClickListener((m, comp) -> {
      comp.getClient().setPanel(new MainPanel());
    });
    row.add(back);

    row.add(getLabel("Tag", 1));
    tags = new ComboBox();
    tags.setSize(200, cellHeight);
    row.add(tags);

    row.add(getLabel("Start Date",2));
    date_start = getTextField(date, 3);
    row.add(date_start);

    row.add(getLabel("Start Time",2));
    time_start = getTextField("00:00:00", 3);
    row.add(time_start);

    row.add(getLabel("End Date", 2));
    date_end = getTextField(date, 3);
    row.add(date_end);

    row.add(getLabel("End Time", 2));
    time_end = getTextField(time, 3);
    row.add(time_end);

    execute = getButton("View Report", 3);
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

  public void init() {
    layout();
    getClient().addResizedListener((cmpnt, i, i1) -> {
      layout();
    });
  }

  private void layout() {
    WebUIClient client = getClient();
    report.setSize(client.getWidth(), client.getHeight() - cellHeight);
  }

  private static final int cellWidth = 32;
  private static final int cellHeight = 32;
  private int nx = 0;
  private Label getLabel(String text, int width) {
    Label lbl = new Label(text);
    lbl.setSize(width * cellWidth, cellHeight);
    lbl.setVerticalAlign(Component.CENTER);
    return lbl;
  }
  private TextField getTextField(String text, int width) {
    TextField tf = new TextField(text);
    tf.setSize(width * cellWidth, cellHeight);
    return tf;
  }
  private Button getButton(String text, int width) {
    Button b = new Button(text);
    b.setSize(width * cellWidth, cellHeight);
    return b;
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
