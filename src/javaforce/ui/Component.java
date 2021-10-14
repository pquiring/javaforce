package javaforce.ui;

/** Component - base class for all UI elements.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.ui.theme.*;

public class Component implements KeyEvents, MouseEvents {
  protected Color foreClr, backClr;
  protected Point pos = new Point();
  protected Dimension size = new Dimension();

  public static final Dimension zero = new Dimension();
  public static final boolean debug = true;

  public Component() {
    Theme theme = Theme.getTheme();
    foreClr = theme.getForeColor();
    backClr = theme.getBackColor();
  }

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

  public void render(Image output) {
    output.fill(pos.x, pos.y, size.width, size.height, backClr.getColor());
  }

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("Component.layout()" + metrics.pos.x + "," + metrics.pos.y + "@" + this);
    pos.x = metrics.pos.x;
    pos.y = metrics.pos.y;
    size.width = getMinWidth();
    size.height = getMinHeight();
  }

  public int mx, my;

  public void keyTyped(char ch) {
    //TODO : focus
  }

  public void keyPressed(int key) {
    //TODO : focus
  }

  public void keyReleased(int key) {
    //TODO : focus
  }

  public void mouseMove(int x, int y) {
//    JFLog.log("mouse:" + x + "," + y);
    mx = x;
    my = y;
  }

  public void mouseDown(int button) {
//    JFLog.log("mousedown:" + button);
  }

  public void mouseUp(int button) {
//    JFLog.log("mouseup:" + button);
  }

  public void mouseScroll(int dx, int dy) {
  }
}
