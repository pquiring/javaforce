package javaforce.webui;

/** SplitPanel - divides two Panels vertically or horizontally.
 *
 * @author pquiring
 */

public class SplitPanel extends Container {

  private int dir;
  private int pos = 50;  //pixels of left/top component

  private Container top;
  private Component div;
  private Block b1, b2;

  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;

  public SplitPanel(int direction) {
    dir = VERTICAL;
    add(b1 = new Block());  //left/top component
    b1.add(new Label(""));
    b1.addClass("splitPanel");
    add(b2 = new Block());  //right/bottom component
    b2.add(new Label(""));
    b2.addClass("splitPanel");
    switch (dir) {
      case VERTICAL:
        top = new Row();
        get(0).setWidth(pos);
        break;
      case HORIZONTAL:
        top = new Column();
        get(0).setHeight(pos);
        break;
    }
    add(top);
    top.add(get(0));  //left/top component
    div = new Block();
    div.setWidth(5);
    div.setBackColor("grey");
    top.add(div);
    top.add(get(1));  //right/bottom component
    get(1).addClass("pad");  //flex
  }

  public void init() {
    super.init();
    div.addEvent("onmousedown", "onmousedownSplitPanel(event, this,\"" + get(0).id + "\",\"" + get(1).id + "\");");
  }

  public String html() {
    return top.html();
  }

  public int getDirection() {
    return dir;
  }

  public void setDividerPosition(int pos) {
    this.pos = pos;
    switch (dir) {
      case VERTICAL: getLeftComponent().setWidth(pos); break;
      case HORIZONTAL: getTopComponent().setHeight(pos); break;
    }
  }

  public int getDividerPosition() {
    return pos;
  }

  public Component getLeftComponent() {
    return b1.get(0);
  }

  public Component getTopComponent() {
    return b1.get(0);
  }

  public Component getRightComponent() {
    return b2.get(0);
  }

  public Component getBottomComponent() {
    return b2.get(0);
  }

  public void setLeftComponent(Component c) {
    b1.set(0, c);
  }

  public void setTopComponent(Component c) {
    b1.set(0, c);
  }

  public void setRightComponent(Component c) {
    c.addClass("pad");
    b2.set(0, c);
  }

  public void setBottomComponent(Component c) {
    c.addClass("pad");
    b2.set(0, c);
  }

}
