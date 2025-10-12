package javaforce.webui;

/** SplitPanel - divides two Panels vertically or horizontally.
 *
 * @author pquiring
 */

public class SplitPanel extends Panel {
  private int dir;
  private int pos = 50;
  private int side;

  private Container div;
  private Container t1, t2;

  private static final int DIV_SIZE = 5;

  /** Create SplitPanel with vertical or horizontal divider.
   *
   * The divider will control the size of the left/top component.
   * The other component will flex as needed.
   *
   * @param direction = VERTICAL or HORIZONTAL
   */
  public SplitPanel(int direction) {
    init(direction, direction == VERTICAL ? RIGHT : BOTTOM);
  }

  /** Create SplitPanel with vertical or horizontal divider.
   *
   * Based on the flexSide param the divider will control the size of the left/top or bottom/right component.
   * The other component will flex as needed.
   *
   * @param direction = VERTICAL or HORIZONTAL
   * @param flexSide = the side that will flex (the divider controls the other component's size)
   *   VERTICAL = LEFT or RIGHT (Default = RIGHT)
   *   HORIZONTAL = TOP or BOTTOM (Default = BOTTOM)
   */
  public SplitPanel(int direction, int flexSide) {
    init(direction, flexSide);
  }

  private void init(int direction, int flexSide) {
    dir = direction;
    side = flexSide;
    div = new Container();
    div.setClass("splitDivider");
    div.setBackColor(Color.grey);
    switch (dir) {
      case VERTICAL:
        setClass("splitPanelRow");
        t1 = new Panel();  //left component
        t1.setMaxHeight();
        t1.setVerticalAlign(TOP);
        t1.add(new Label("left"));

        t2 = new Panel();  //right component
        t2.setMaxHeight();
        t2.setVerticalAlign(TOP);
        t2.add(new Label("right"));

        div.setWidth(DIV_SIZE);
        div.setMaxHeight();

        switch (side) {
          case LEFT:
            t1.setFlex(true);
//            t1.setMaxWidth();
            t2.setWidth(pos);
            break;
          case RIGHT:
            t1.setWidth(pos);
            t2.setFlex(true);
//            t2.setMaxWidth();
            break;
        }
        break;
      case HORIZONTAL:
        setClass("splitPanelColumn");
        t1 = new Panel();  //top component
        t1.setMaxWidth();
        t1.setVerticalAlign(TOP);
        t1.add(new Label("top"));

        t2 = new Panel();  //bottom component
        t2.setMaxHeight();
        t2.setVerticalAlign(TOP);
        t2.add(new Label("bottom"));

        div.setHeight(DIV_SIZE);
        div.setMaxWidth();

        switch (side) {
          case TOP:
            t1.setFlex(true);
//            t1.setMaxHeight();
            t2.setHeight(pos);
            break;
          case BOTTOM:
            t1.setHeight(pos);
            t2.setFlex(true);
//            t2.setMaxHeight();
            break;
        }
        break;
    }
    add(t1);  //left/top component
    add(div);
    add(t2);  //right/bottom component
    setMaxWidth();
    setMaxHeight();
  }

  public void init() {
    super.init();
    div.addEvent("onmousedown", "onmousedownSplitPanel("
      + "event,"
      + "this,"
      + "\"" + t1.id + "\","
      + "\"" + div.id + "\","
      + "\"" + t2.id + "\","
      + "\"" + this.id + "\","
      + "\"" + (dir == VERTICAL ? 'v' : 'h') + "\","
      + "\"" + (dir == VERTICAL ? (side == LEFT ? 'l' : 'r') : (side == TOP ? 't' : 'b')) + "\""
      + ");"
    );
  }

  //

  public int getDirection() {
    return dir;
  }

  public void setDividerPosition(int pos) {
    this.pos = pos;
    switch (dir) {
      case VERTICAL:
        switch (side) {
          case LEFT: t2.setWidth(pos); break;
          case RIGHT: t1.setWidth(pos); break;
        }
        break;
      case HORIZONTAL:
        switch (side) {
          case TOP: t2.setHeight(pos); break;
          case BOTTOM: t1.setHeight(pos); break;
        }
        break;
    }
  }

  public int getDividerPosition() {
    return pos;
  }

  public Component getLeftComponent() {
    return t1.get(0);
  }

  public Component getTopComponent() {
    return t1.get(0);
  }

  public Component getRightComponent() {
    return t2.get(0);
  }

  public Component getBottomComponent() {
    return t2.get(0);
  }

  public void setLeftComponent(Component c) {
    t1.set(0, c);
    this.sendOnResize();
  }

  public void setTopComponent(Component c) {
    t1.set(0, c);
    this.sendOnResize();
  }

  public void setRightComponent(Component c) {
    t2.set(0, c);
    this.sendOnResize();
  }

  public void setBottomComponent(Component c) {
    t2.set(0, c);
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
