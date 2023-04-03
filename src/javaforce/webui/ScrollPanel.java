package javaforce.webui;

/** Scroll Panel
 *
 * @author pquiring
 */

public class ScrollPanel extends Panel {
  private Container cell;
  private Container ctr;
  public ScrollPanel() {
    setOverflow(SCROLL);
    setClass("scrollpanel");
    cell = new Panel();
    cell.addClass("scrollpanelcell");
    ctr = new Panel();
    ctr.setClass("block");
    ctr.setWidth(0);
    ctr.setHeight(0);
    cell.add(ctr);
    super.add(cell);
  }
  public void onLoaded(String args[]) {
    super.onLoaded(args);
    ctr.sendEvent("setsizetoparent2", null);
  }
  public void add(Component cmp) {
    ctr.add(cmp);
  }
  public void add(int idx, Component cmp) {
    ctr.add(idx, cmp);
  }
  public void remove(Component cmp) {
    ctr.remove(cmp);
  }
  public void remove(int idx) {
    ctr.remove(idx);
  }
}
