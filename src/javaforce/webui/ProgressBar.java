package javaforce.webui;

/** Progress bar.
 *
 * @author pquiring
 */

public class ProgressBar extends Container {
  public int clr0, clr1, clr2;
  public float lvl0, lvl1;
  private int dir;
  private float max;
  private float value;
  private Block bar;
  private int barWidth;
  public ProgressBar(int dir, float max, int barWidth) {
    this.barWidth = barWidth;
    value = 0;
    this.max = max;
    bar = new Block();
    add(bar);
    setBackColor(Color.grey);
    setDir(dir);
    switch (dir) {
      case HORIZONTAL: setSize(100, barWidth); break;
      case VERTICAL: setSize(barWidth, 100); break;
    }
    setSize();
    clr0 = Color.red;
    clr1 = Color.yellow;
    clr2 = Color.green;
    lvl0 = 5;
    lvl1 = 10;
    setColor();
  }
  public void setBarWidth(int size) {
    barWidth = size;
  }
  private void setSize() {
    switch (dir) {
      case HORIZONTAL:
        setSize(width, barWidth);
        bar.setSize((int)(value / max * width), barWidth);
        break;
      case VERTICAL:
        setSize(barWidth, height);
        bar.setSize(barWidth, (int)(value / max * height));
        break;
    }
  }
  public void setColors(int clr0, int clr1, int clr2) {
    this.clr0 = clr0;
    this.clr1 = clr1;
    this.clr2 = clr2;
    setColor();
  }
  public void setLevels(float lvl0, float lvl1, float max) {
    this.lvl0 = lvl0;
    this.lvl1 = lvl1;
    this.max = max;
    setColor();
  }
  public void setValue(float value) {
    if (value < 0) value = 0;
    if (value > max) value = max;
    this.value = value;
    setSize();
    setColor();
  }
  public float getValue() {
    return value;
  }
  private void setColor() {
    if (value <= lvl0) {
      bar.setBackColor(clr0);
    } else if (value <= lvl1) {
      bar.setBackColor(clr1);
    } else {
      bar.setBackColor(clr2);
    }
  }
  public void setDir(int dir) {
    this.dir = dir;
    switch (dir) {
      case VERTICAL:
        setClass("");
        setStyle("margin-top", max + "px");
        break;
      case HORIZONTAL:
        setClass("");
        setStyle("margin-top", "0px");
        break;
    }
  }
}
