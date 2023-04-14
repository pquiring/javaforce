package javaforce.webui;

/** Scroll Panel
 *
 * @author pquiring
 */

public class ScrollPanel extends Panel {
  private Panel cell;
  private Panel ctr;
  public ScrollPanel() {
    setOverflow(SCROLL);
    setClass("scrollpanel");
    setResizeChild(false);
    cell = new Panel();
    cell.addClass("scrollpanelcell");
//    cell.setResizeChild(false);
    ctr = new Panel();
    ctr.setClass("block");
//    ctr.setResizeChild(false);
    ctr.setOverflow(AUTO);
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
