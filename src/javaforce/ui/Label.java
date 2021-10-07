package javaforce.ui;

/** Label
 *
 * @author pquiring
 */

public class Label extends TextComponent {
  private String label;
  public Label(String label) {
    this.label = label;
  }
  public Dimension getPreferredSize() {
    FontMetrics metrics = getFont().getMetrics(label);
    return new Dimension(metrics.getWidth(), metrics.getHeight());
  }
  public void render(Image image) {
    getFont().drawText(x, y - getFont().getMaxAscent(), label, image);
  }
}
