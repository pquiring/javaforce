package javaforce.ui;

/** LayoutMetrics.
 *
 * @author pquiring
 */

public class LayoutMetrics {
  public Point pos = new Point();
  public Dimension size = new Dimension();

  public LayoutMetrics() {
  }
  public LayoutMetrics(int width, int height) {
    size.width = width;
    size.height = height;
  }

  public void setPosition(int x, int y) {
    pos.x = x;
    pos.y = y;
  }

  public void setPostion(Point pt) {
    setPosition(pt.x, pt.y);
  }

  public void setSize(int width, int height) {
    size.width = width;
    size.height = height;
  }

  public void setSize(Dimension dim) {
    setSize(dim.width, dim.height);
  }
}
