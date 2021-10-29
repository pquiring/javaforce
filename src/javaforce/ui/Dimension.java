package javaforce.ui;

/** Dimension
 *
 * @author pquiring
 */

public class Dimension {
  public int width, height;

  public Dimension() {}

  public Dimension(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public String toString() {
    return "Dimension:" + width + "x" + height;
  }
}
