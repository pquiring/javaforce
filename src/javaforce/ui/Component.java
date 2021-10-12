package javaforce.ui;

/** Component - base class for all UI elements.
 *
 * @author pquiring
 */

import javaforce.*;

public class Component {
  protected Font font;
  protected Color foreClr, backClr;
  protected Point pos = new Point();
  protected Dimension size = new Dimension();
  public static final Dimension zero = new Dimension();

  public static final boolean debug = true;

  /** Loads a complete form from a XML file (*.ui). */
  public static Component load(String fn) {
    return null;
  }

  /** Saves a component (and all children) to an XML file (*.ui). */
  public void save(String fn) {

  }

  public Point getPosition() {
    return pos;
  }

  public int getX() {
    return pos.x;
  }

  public int getY() {
    return pos.y;
  }

  public void setPosition(Point pt) {
    pos.x = pt.x;
    pos.y = pt.y;
  }

  public void setPosition(int x, int y) {
    pos.x = x;
    pos.y = y;
  }

  public Dimension getSize() {
    return size;
  }

  public int getWidth() {return size.width;}

  public int getHeight() {return size.height;}

  public void setSize(Dimension dim) {
    size.width = dim.width;
    size.height = dim.height;
  }

  public void setSize(int width, int height) {
    size.width = width;
    size.height = height;
  }

  public Dimension getMinSize() {
    return size;
  }

  public int getMinWidth() {
    return getMinSize().width;
  }

  public int getMinHeight() {
    return getMinSize().height;
  }

  public void render(Image output) {}

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("Component.layout()" + metrics.pos.x + "," + metrics.pos.y + "@" + this);
    pos.x = metrics.pos.x;
    pos.y = metrics.pos.y;
  }
}
