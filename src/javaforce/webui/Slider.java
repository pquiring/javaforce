package javaforce.webui;

/** Slider
 *
 * @author pquiring
 */

public class Slider extends Component {
  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;
  private int dir, min, max, step;
  public Slider(int dir, int min, int max, int step) {
    this.dir = dir;
    this.min = min;
    this.max = max;
    this.step = step;
    addAttr("min", Integer.toString(min));
    addAttr("max", Integer.toString(max));
    addAttr("step", Integer.toString(step));
    switch (dir) {
      case VERTICAL: setClass("vslider"); break;
      case HORIZONTAL: setClass("hslider"); break;
    }
  }
  public String html() {
    return "<input type='range'" + getAttrs() + ">";
  }
}
