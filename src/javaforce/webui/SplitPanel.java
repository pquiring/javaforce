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
  private Container b1, b2;

  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;

  public SplitPanel(int direction) {
    dir = direction;
    div = new Block();
    div.setClass("splitDivider");
    div.setBackColor(Color.grey);
    switch (dir) {
      case VERTICAL:
        b1 = new Column();  //left component
        b1.add(new Label("left"));
        b1.addClass("splitPanel");
        b2 = new Column();  //right component
        b2.add(new Label("right"));
        b2.addClass("splitPanel");
        top = new Row();
        b1.setWidth(pos);
        div.setWidth(5);
        break;
      case HORIZONTAL:
        b1 = new Row();  //top component
        b1.add(new Label("top"));
        b1.addClass("splitPanel");
        b2 = new Row();  //bottom component
        b2.add(new Label("bottom"));
        b2.addClass("splitPanel");
        top = new Column();
        b1.setHeight(pos);
        div.setHeight(5);
        break;
    }
    top.add(b1);  //left/top component
    top.add(div);
    top.add(b2);  //right/bottom component
    add(top);
  }

  public void init() {
    super.init();
    div.addEvent("onmousedown", "onmousedownSplitPanel(event, this,\"" + b1.id + "\",\"" + b2.id + "\");");
    switch(dir) {
      case VERTICAL:
        top.addEvent("onresize", "onresizeSplitDividerHeight(event, this,\"" + b1.id + "\",\"" + b2.id + "\",\"" + div.id + "\")");
        break;
      case HORIZONTAL:
        top.addEvent("onresize", "onresizeSplitDividerWidth(event, this,\"" + b1.id + "\",\"" + b2.id + "\",\"" + div.id + "\")");
        break;
    }
  }

  public void onLoaded(String args[]) {
    super.onLoaded(args);
    sendEvent("setheighttoparent", null);
    top.sendEvent("setheighttoparent", null);
    div.sendEvent("setheighttoparent", null);
  }

  public int getDirection() {
    return dir;
  }

  public void setDividerPosition(int pos) {
    this.pos = pos;
    switch (dir) {
      case VERTICAL:
        b1.setWidth(pos);
        getLeftComponent().setWidth(pos);
        break;
      case HORIZONTAL:
        b1.setHeight(pos);
        getTopComponent().setHeight(pos);
        break;
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
    b2.set(0, c);
  }

  public void setBottomComponent(Component c) {
    b2.set(0, c);
  }
}
