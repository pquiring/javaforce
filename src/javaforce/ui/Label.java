package javaforce.ui;

/** Label
 *
 * @author pquiring
 */

import javaforce.*;

public class Label extends TextComponent {
  private String text;
  public Label(String label) {
    this.text = label;
  }
  public Dimension getMinSize() {
    FontMetrics metrics = getFont().getMetrics(text);
    if (debug) JFLog.log("Font.size=" + metrics.getWidth() + "," + metrics.getHeight());
    return new Dimension(metrics.getWidth(), metrics.getHeight());
  }
  public void render(Image image) {
    getFont().drawText(pos.x, pos.y - getFont().getMaxAscent(), text, image);
  }
  public String toString() {
    return "Label:" + text;
  }
}
