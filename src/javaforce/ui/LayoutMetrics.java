package javaforce.ui;

/** LayoutMetrics.
 *
 * @author pquiring
 */

public class LayoutMetrics {
  public Point pos = new Point();
  public Dimension size = new Dimension();

  public LayoutMetrics(int width, int height) {
    size.width = width;
    size.height = height;
  }
}
