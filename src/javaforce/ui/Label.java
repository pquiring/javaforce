package javaforce.ui;

/** Label
 *
 * @author pquiring
 */

public class Label extends TextComponent {
  public Label(String text) {
    super(false);
    setText(text);
  }
  public Dimension getMinSize() {
    FontMetrics metrics = getFont().getMetrics(getText());
    return new Dimension(metrics.getWidth(), metrics.getHeight());
  }
  public void render(Image image) {
    image.setForeColor(getForeColor());
    image.drawText(pos.x, pos.y - getFont().getMaxAscent(), getText());
  }
  public String toString() {
    return "Label:" + getText();
  }
}
