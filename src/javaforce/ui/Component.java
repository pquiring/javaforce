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

  protected Image loadImage(String name) {
    try {
      Image image = new Image();
      if (!image.loadPNG(getClass().getResourceAsStream(name))) {
        throw new Exception("Load resource failed:" + name);
      }
      return image;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
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
    //NOTE : setSize(int,int) may be overrided
    setSize(dim.width, dim.height);
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

  public boolean isInside(Point pt) {
    int x1 = pos.x;
    int y1 = pos.y;
    int x2 = x1 + size.width - 1;
    int y2 = y1 + size.height - 1;
    return (pt.x >= x1 && pt.x <= x2 && pt.y >= y1 && pt.y <= y2);
  }

  public void render(Image image) {
    image.fill(pos.x, pos.y, size.width, size.height, getBackColor().getColor());
  }

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("layout:" + metrics.size.width + "x" + metrics.size.height + "@" + metrics.pos.x + "," + metrics.pos.y + ":" + this);
    setPosition(metrics.pos);
    setSize(metrics.size);
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

  public int getMouseX() {
    return mx;
  }

  public int getMouseY() {
    return my;
  }

  public boolean getKeyState(int vk) {
    if (vk < 0 || vk >= 256) return false;
    return keyState[vk];
  }

  private int mx, my;
  private static boolean[] keyState = new boolean[256];

  //keyboard input

  public void keyTyped(char ch) {
  }

  public void keyPressed(int key) {
    if (key >= 0 && key < 256) {
      keyState[key] = true;
    }
  }

  public void keyReleased(int key) {
    if (key >= 0 && key < 256) {
      keyState[key] = false;
    }
  }

  //mouse input

  public void mouseMove(int x, int y) {
    mx = x;
    my = y;
  }

  public void mouseDown(int button) {
    if (isFocusable()) {
      setFocus();
    }
  }

  public void mouseUp(int button) {
  }

  public void mouseScroll(int dx, int dy) {
  }
}
