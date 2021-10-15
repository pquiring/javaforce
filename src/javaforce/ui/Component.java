package javaforce.ui;

/** Component - base class for all UI elements.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.ui.theme.*;

public class Component implements KeyEvents, MouseEvents {
  protected Color foreClr;
  protected Color backClr;
  protected Color disabledClr;
  protected Color selectedClr;
  protected Point pos = new Point();
  protected Dimension size = new Dimension();
  protected Component parent;
  protected boolean enabled = true;
  protected boolean focused = false;
  protected boolean focusable = false;

  public static final Dimension zero = new Dimension();
  public static final boolean debug = true;

  public Component() {
    Theme theme = Theme.getTheme();
    setForeColor(theme.getForeColor());
    setBackColor(theme.getBackColor());
    setDisabledColor(theme.getDisabledColor());
    setSelectedColor(theme.getSelectedColor());
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
    output.fill(pos.x, pos.y, size.width, size.height, getBackColor().getColor());
  }

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("Component.layout()" + metrics.pos.x + "," + metrics.pos.y + "@" + this);
    pos.x = metrics.pos.x;
    pos.y = metrics.pos.y;
    size.width = getMinWidth();
    size.height = getMinHeight();
  }

  public void setFocusable(boolean state) {
    if (focusable == state) return;  //no change
    focusable = state;
/*/
    if (focusable) {
      //add to focus list
      getTopContainer().addFocusable(this);
    } else {
      //remove from focus list
      getTopContainer().removeFocusable(this);
    }
/*/
  }

  public Container getTopContainer() {
    Component top = parent;
    while (top.parent != null) {
      top = parent.parent;
    }
    return (Container)top;
  }

  public void onFocus() {
    JFLog.log("onFonus:" + this);
    focused = true;
  }

  public void onBlur() {
    JFLog.log("onBlur:" + this);
    focused = false;
  }

  public void setFocus() {
    getTopContainer().setFocus(this);
  }

  public boolean isFocused() {
    return focused;
  }

  public boolean isFocusable() {
    return focusable;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean state) {
    enabled = state;
  }

  public Color getForeColor() {
    return foreClr;
  }

  public Color getBackColor() {
    return backClr;
  }

  public Color getDisabledColor() {
    return disabledClr;
  }

  public Color getSelectedColor() {
    return selectedClr;
  }

  public void setForeColor(Color clr) {
    foreClr = clr;
  }

  public void setBackColor(Color clr) {
    backClr = clr;
  }

  public void setDisabledColor(Color clr) {
    disabledClr = clr;
  }

  public void setSelectedColor(Color clr) {
    selectedClr = clr;
  }

  public int mx, my;

  public void keyTyped(char ch) {
  }

  public void keyPressed(int key) {
  }

  public void keyReleased(int key) {
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
