package javaforce.webui;

/** Rectangle
 *
 * @author pquiring
 */

public class Rectangle {
  public int x,y;
  public int width, height;
  public Rectangle() {}
  public Rectangle(int x,int y,int width,int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }
  public String toString() {
    return "Rectangle:x=" + x + ",y=" + y + ",width=" + width + ",height=" + height;
  }
}
