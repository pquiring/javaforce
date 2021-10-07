package javaforce.ui;

/** Component - base class for all UI elements.
 *
 * @author pquiring
 */

public class Component {
  protected Font font;
  protected Color foreClr, backClr;
  protected int x, y;  //position
  protected int width, height;  //size
  protected int pwidth, pheight;  //preferred size

  /** Loads a complete form from a XML file (*.ui). */
  public static Component load(String fn) {
    return null;
  }

  /** Saves a component (and all children) to an XML file (*.ui). */
  public void save(String fn) {

  }

  public int getWidth() {return width;}

  public int getHeight() {return height;}

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public Dimension getSize() {
    return new Dimension(width, height);
  }

  public Dimension getPreferredSize() {
    return new Dimension(width, height);
  }

  public void render(Image output) {}

  public void layout(LayoutMetrics metrics) {
    x = metrics.x;
    y = metrics.y;
    Dimension size = getPreferredSize();
    width = size.width;
    height = size.height;
    metrics.x += width;
  }
}
