package javaforce.webui;

/** Tabbed Panel
 *
 * @author pquiring
 */

public class TabPanel extends Column {
  private Column tabs;
  private Row row;
  private int idx;
  private boolean borders = true;
  private boolean tabsVisible = true;
  public TabPanel() {
    addClass("tab");
    setBorderGray(true);
    row = new Row();
    add(row);
    tabs = new Column();
    add(tabs);
  }
  /** Creates borders on added tabs. */
  public void setBorders(boolean state) {
    borders = state;
    setBorderGray(state);
  }
  public void add(Panel panel, String text) {
    int cnt = row.count();
    Label label = new Label(text);
    label.setClass("tab" + (cnt == 0 ? "active" : "inactive"));
    if (borders) label.setBorderGray(true);
    row.add(label);
    tabs.add(panel);
    panel.setClass("tabcontent" + (cnt == 0 ? "shown" : "hidden"));
    if (borders) panel.setBorderGray(true);
  }
  /** Shows or hides the tabs so only the panels are visible.
   *  If the tabs are not visible the user can not change tabs.
   */
  public void setTabsVisible(boolean state) {
    if (tabsVisible == state) return;
    tabsVisible = state;
    if (state) {
      add(0, row);
    } else {
      remove(row);
    }
  }
  public void init() {
    super.init();
    int cnt = row.count();
    for(int a=0;a<cnt;a++) {
      Component c = row.get(a);
      c.addEvent("onclick", "openTab(event, " + a + ",\"" + tabs.id + "\",\"" + row.id + "\");");
    }
  }
  public void setTabIndex(int idx) {
    this.idx = idx;
    sendEvent("settab", new String[] {"tabs=" + tabs.id, "row=" + row.id, "idx=" + idx});
  }
  public int getTabIndex() {
    return idx;
  }
  public int getTabsCount() {
    return tabs.count();
  }
  public void removeTab(int idx) {
    row.remove(idx);
    tabs.remove(idx);
  }
}
