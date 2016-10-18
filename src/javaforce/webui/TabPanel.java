package javaforce.webui;

/** Tabbed Panel
 *
 * @author pquiring
 */

public class TabPanel extends Container {
  private Column top, tabs;
  private Row row;
  public TabPanel() {
    top = new Column();
    add(top);
    top.addClass("tab");
    row = new Row();
    add(row);
    top.add(row);
    tabs = new Column();
    add(tabs);
    top.add(tabs);
  }
  public void add(Panel panel, String text) {
    add(panel);
    int cnt = row.count();
    Label label = new Label(text);
    label.addEvent("onclick", "openTab(this," + cnt + ");");
    label.setClass("tab" + (cnt == 0 ? "active" : "inactive"));
    row.add(label);
    tabs.add(panel);
    panel.setClass("tabcontent" + (cnt == 0 ? "shown" : "hidden"));
  }
  public String html() {
    return top.html();
  }
}
