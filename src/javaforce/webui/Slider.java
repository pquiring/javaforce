package javaforce.webui;

/** Slider
 *
 * @author pquiring
 */

public class Slider extends Container {
  private int dir, min, max, pos;
  private int size;
  private Block bar;
  public Slider(int dir, int min, int max, int pos) {
    this.dir = dir;
    this.min = min;
    this.max = max;
    this.pos = pos;
    size = max - min;
    bar = new Block();
    bar.setClass("slider");
    bar.setSize(16, 16);
    add(bar);
    setBackColor(Color.grey);
    bar.setBackColor(Color.darkBlue);
    switch (dir) {
      case HORIZONTAL:
        setSize(size + 16, 16);
        bar.setStyle("margin-left", pos + "px");
        break;
      case VERTICAL:
        setSize(16, size + 16);
        bar.setStyle("margin-top", pos + "px");
        break;
    }
  }
  public void onEvent(String event, String[] args) {
    switch (event) {
      case "sliderpos":
        for(int a=0;a<args.length;a++) {
          if (args[a].startsWith("pos=")) {
            pos = Integer.valueOf(args[a].substring(4));
            onChanged(null);
          }
        }
        break;
    }
  }
  public void init() {
    super.init();
    switch (dir) {
      case HORIZONTAL:
        bar.addEvent("onmousedown", "onmousedownSlider(event, this,'" + id + "','h'," + size + ");");
        break;
      case VERTICAL:
        bar.addEvent("onmousedown", "onmousedownSlider(event, this,'" + id + "','v'," + size + ");");
        break;
    }
  }
  public void setPos(int pos) {
    if (pos < min) pos = min;
    if (pos > max) pos = max;
    this.pos = pos;
    switch (dir) {
      case HORIZONTAL:
        bar.sendEvent("setmarginleft", new String[] {"px=" + pos});
        break;
      case VERTICAL:
        bar.sendEvent("setmargintop", new String[] {"px=" + pos});
        break;
    }
  }
  public int getPos() {
    return pos;
  }
}
