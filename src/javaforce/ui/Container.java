package javaforce.ui;

/** Container - contains other Components.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public abstract class Container extends Component {
  public ArrayList<Component> children;
  public Component focus;
  public Dimension minSize = new Dimension();

  public Container() {
    children = new ArrayList<>();
  }

  public void add(Component child) {
    children.add(child);
    child.parent = this;
  }

  public void remove(Component child) {
    children.remove(child);
    child.parent = null;
  }

  public abstract Dimension getMinSize();

  /** Lays out components. */
  public abstract void layout(LayoutMetrics metrics);

  public void render(Image output) {
    super.render(output);
    for(Component child : children) {
      child.render(output);
    }
  }

  private Component firstFocus() {
    JFLog.log("firstFocus:" + this);
    for(Component child : children) {
      if (child.isFocusable()) {
        JFLog.log("Focus=" + child);
        return child;
      }
      if (child instanceof Container) {
        Component c = ((Container)child).firstFocus();
        if (c != null) return c;
      }
    }
    return null;
  }

  private Component nextFocus(Component focus, boolean found) {
    JFLog.log("nextFocus:" + this);
    for(Component child : children) {
      if (!found) {
        if (child == focus) {
          found = true;
        } else {
          if (child instanceof Container) {
            Component c = ((Container)child).nextFocus(focus, found);
            if (c != null) return c;
          }
        }
        continue;
      }
      if (child.isFocusable()) {
        JFLog.log("Focus=" + child);
        return child;
      }
      if (child instanceof Container) {
        Component c = ((Container)child).nextFocus(focus, found);
        if (c != null) return c;
      }
    }
    return null;
  }

  private void nextFocus() {
    JFLog.log("nextFocus");
    if (focus != null) {
      focus.onBlur();
      focus = nextFocus(focus, false);
      if (focus == null) {
        focus = firstFocus();
      }
    } else {
      focus = firstFocus();
    }
    if (focus != null) {
      focus.onFocus();
    }
  }

  public void setFocus(Component child) {
    if (focus != null) {
      focus.onBlur();
    }
    focus = child;
    focus.onFocus();
  }

  public void keyTyped(char ch) {
    if (debug) JFLog.log("keyTyped:" + (int)ch);
    if (focus != null) {
      focus.keyTyped(ch);
    }
  }

  public void keyPressed(int key) {
    if (debug) JFLog.log("keyPressed:" + key);
    switch (key) {
      case KeyCode.VK_TAB:
        nextFocus();
        break;
    }
    if (focus != null) {
      focus.keyPressed(key);
    }
  }

  public void keyReleased(int key) {
    if (debug) JFLog.log("keyReleased:" + key);
    if (focus != null) {
      focus.keyReleased(key);
    }
  }

  public void mouseMove(int x, int y) {
    super.mouseMove(x, y);
    for(Component child : children) {
      Point pos = child.getPosition();
      Dimension size = child.getSize();
      int x1 = pos.x;
      int y1 = pos.y;
      int x2 = x1 + size.width - 1;
      int y2 = y1 + size.height - 1;
      if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
        child.mouseMove(x, y);
      }
    }
  }

  public void mouseDown(int button) {
    int x = mx;
    int y = my;
    super.mouseDown(button);
    for(Component child : children) {
      Point pos = child.getPosition();
      Dimension size = child.getSize();
      int x1 = pos.x;
      int y1 = pos.y;
      int x2 = x1 + size.width - 1;
      int y2 = y1 + size.height - 1;
      if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
        child.mouseDown(button);
      }
    }
  }

  public void mouseUp(int button) {
    int x = mx;
    int y = my;
    super.mouseUp(button);
    for(Component child : children) {
      Point pos = child.getPosition();
      Dimension size = child.getSize();
      int x1 = pos.x;
      int y1 = pos.y;
      int x2 = x1 + size.width - 1;
      int y2 = y1 + size.height - 1;
      if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
        child.mouseUp(button);
      }
    }
  }

  public void mouseScroll(int dx, int dy) {
    int x = mx;
    int y = my;
    super.mouseScroll(dx, dy);
    for(Component child : children) {
      Point pos = child.getPosition();
      Dimension size = child.getSize();
      int x1 = pos.x;
      int y1 = pos.y;
      int x2 = x1 + size.width - 1;
      int y2 = y1 + size.height - 1;
      if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
        child.mouseScroll(dx, dy);
      }
    }
  }
}
