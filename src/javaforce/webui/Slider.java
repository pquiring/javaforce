package javaforce.webui;

/** Slider
 *
 * Requires IE10+
 *
 * @author pquiring
 */

public class Slider extends Component {
  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;
  private int dir, min, max, step, pos;
  public Slider(int dir, int min, int max, int step) {
    this.dir = dir;
    this.min = min;
    this.max = max;
    this.step = step;
    addAttr("min", Integer.toString(min));
    addAttr("max", Integer.toString(max));
    addAttr("step", Integer.toString(step));
    int delta = max - min;
    setWidth((delta + 32) + "px");
    setHeight("32px");
    switch (dir) {
      case VERTICAL:
        setClass("vslider");
        this.setStyle("margin-top", delta + "px");
        break;
      case HORIZONTAL:
        setClass("hslider");
        break;
    }
    addEvent("onchange", "onSliderMove(event, this);");
  }
  public void setPos(int pos) {
    if (pos < min) pos = min;
    if (pos > max) pos = max;
    this.pos = pos;
    getClient().sendEvent(id, "setpos", new String[] {"pos=" + pos});
  }
  public int getPos() {
    return pos;
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div class=cslider");  //slider container
    int delta = max - min;
    delta += 32;
    switch (dir) {
      case VERTICAL:
        sb.append(" style='width:32px; height:" + delta + "px;'");
        break;
      case HORIZONTAL:
        break;
    }
    sb.append(">");
    sb.append("<input type='range'" + getAttrs() + ">");
    sb.append("</div>");
    return sb.toString();
  }
  public void onChanged(String args[]) {
    int idx = args[0].indexOf("=");
    pos = Integer.valueOf(args[0].substring(idx+1));
  }
}
