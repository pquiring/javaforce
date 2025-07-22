package javaforce.webui;

/** SplitPanel - divides two Panels vertically or horizontally.
 *
 * @author pquiring
 */

public class SplitPanel extends Panel {
  private int dir;
  private int pos = 50;  //pixels of left/top component

  private Container div;
  private Container t1, t2;  //NOTE:table-cells ignore specified sizes
  private Container c1, c2;

  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;

  public SplitPanel(int direction) {
    dir = direction;
    div = new Container();
    div.setClass("splitDivider");
    div.setBackColor(Color.grey);
    switch (dir) {
      case VERTICAL:
        t1 = new Panel();  //left component
        t1.setWidth(pos);
        t1.setMaxHeight();
        t1.addClass("splitPanelCell");
        t1.setVerticalAlign(TOP);
        c1 = new Panel();
        c1.setWidth(pos);
        c1.setMaxHeight();
        c1.addClass("block");
        c1.add(new Label("left"));
        t1.add(c1);

        t2 = new Panel();  //right component
        t2.setMaxWidth();
        t2.setMaxHeight();
        t2.addClass("splitPanelCell");
        t2.setVerticalAlign(TOP);
        c2 = new Panel();
        c2.setMaxWidth();
        c2.setMaxHeight();
        c2.addClass("block");
        c2.add(new Label("right"));
        t2.add(c2);

        div.setWidth(5);
        div.setMaxHeight();
        break;
      case HORIZONTAL:
        t1 = new Panel();  //top component
        t1.setHeight(pos);
        t1.setMaxWidth();
        t1.addClass("splitPanelRow");
        t1.setVerticalAlign(TOP);
        c1 = new Panel();
        c1.setHeight(pos);
        c1.setMaxWidth();
        c1.addClass("block");
        c1.add(new Label("top"));
        t1.add(c1);

        t2 = new Panel();  //bottom component
        t2.setMaxWidth();
        t2.setMaxHeight();
        t2.addClass("splitPanelRow");
        t2.setVerticalAlign(TOP);
        c2 = new Panel();
        c2.setMaxWidth();
        c2.setMaxHeight();
        c2.addClass("block");
        c2.add(new Label("bottom"));
        t2.add(c2);

        div.setMaxWidth();
        div.setHeight(5);
        break;
    }
    add(t1);  //left/top component
    add(div);
    add(t2);  //right/bottom component
    addClass("splitPanelTop");
    setMaxWidth();
    setMaxHeight();
  }

  public void init() {
    super.init();
    switch(dir) {
      case VERTICAL:
        div.addEvent("onmousedown", "onmousedownSplitPanel(event, this,\"" + c1.id + "\",\"" + div.id + "\",\"" + c2.id + "\",\"" + this.id + "\", \"v\");");
        addEvent("onresize", "onresizeSplitPanelWidth(event, this,\"" + c1.id + "\",\"" + div.id + "\",\"" + c2.id + "\")");
        break;
      case HORIZONTAL:
        div.addEvent("onmousedown", "onmousedownSplitPanel(event, this,\"" + c1.id + "\",\"" + div.id + "\",\"" + c2.id + "\",\"" + this.id + "\", \"h\");");
        addEvent("onresize", "onresizeSplitPanelHeight(event, this,\"" + c1.id + "\",\"" + div.id + "\",\"" + c2.id + "\")");
        break;
    }
  }

  public int getDirection() {
    return dir;
  }

  public void setDividerPosition(int pos) {
    this.pos = pos;
    switch (dir) {
      case VERTICAL:
        t1.setWidth(pos);
        c1.setWidth(pos);
        getLeftComponent().setWidth(pos);
        break;
      case HORIZONTAL:
        t1.setHeight(pos);
        c1.setHeight(pos);
        getTopComponent().setHeight(pos);
        break;
    }
  }

  public int getDividerPosition() {
    return pos;
  }

  public Component getLeftComponent() {
    return c1.get(0);
  }

  public Component getTopComponent() {
    return c1.get(0);
  }

  public Component getRightComponent() {
    return c2.get(0);
  }

  public Component getBottomComponent() {
    return c2.get(0);
  }

  public void setLeftComponent(Component c) {
    c1.set(0, c);
    this.sendOnResize();
  }

  public void setTopComponent(Component c) {
    c1.set(0, c);
    this.sendOnResize();
  }

  public void setRightComponent(Component c) {
    c2.set(0, c);
    this.sendOnResize();
  }

  public void setBottomComponent(Component c) {
    c2.set(0, c);
    this.sendOnResize();
  }

  public void onEvent(String event, String[] args) {
    switch (event) {
      case "dividerpos":
        for(String arg : args) {
          if (arg.startsWith("pos=")) {
            pos = Integer.valueOf(arg.substring(4));
            onChanged(null);
          }
        }
        break;
    }
  }
}
