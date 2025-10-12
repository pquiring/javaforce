package javaforce.webui;

/** Tabbed Panel
 *
 * @author pquiring
 */

public class TabPanel extends Panel {
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
    tabs.setMaxWidth();
    tabs.setMaxHeight();
    add(tabs);
    setAlign(Component.LEFT);
  }
  /** Creates borders on added tabs. */
  public void setBorders(boolean state) {
    borders = state;
    setBorderGray(state);
  }
  public void addTab(Panel panel, String text) {
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
      c.addEvent("onclick", "openTab(event," + a + ",\"" + id + "\",\"" + row.id + "\",\"" + tabs.id + "\");");
    }
  }
  public void onLoaded(String[] args) {
    super.onLoaded(args);
    sendOnResize();
  }
  public void setTabIndex(int idx) {
    if (idx == this.idx) return;
    Label currentLbl = (Label)row.get(this.idx);
    currentLbl.removeClass("tabactive");
    currentLbl.addClass("tabinactive");

    Label newLbl = (Label)row.get(idx);
    newLbl.removeClass("tabinactive");
    newLbl.addClass("tabactive");

    Panel currentPanel = (Panel)tabs.get(this.idx);
    currentPanel.removeClass("tabcontentshown");
    currentPanel.addClass("tabcontenthidden");

    Panel newPanel = (Panel)tabs.get(idx);
    newPanel.removeClass("tabcontenthidden");
    newPanel.addClass("tabcontentshown");

    this.idx = idx;
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
