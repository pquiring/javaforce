package javaforce.ui;

/** Point - 2D Position
 *
 * @author pquiring
 */

public class Point {
  public int x, y;
  public Point() {
  }
  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }
  public String toString() {
    return "Point:" + x + "," + y;
  }
}
