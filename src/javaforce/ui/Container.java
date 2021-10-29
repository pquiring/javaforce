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
    setConsumer(false);
  }

  public boolean isContainer() {
    return true;
  }

  public void add(Component child) {
    children.add(child);
    child.parent = this;
  }

  public void remove(Component child) {
    children.remove(child);
    child.parent = null;
  }

  public Component getChild(int idx) {
    return children.get(idx);
  }

  public int getChildCount() {
    return children.size();
  }

  public Component[] getChildren() {
    return children.toArray(new Component[children.size()]);
  }

  public <T> T[] getChildren(T[] array) {
    return children.toArray(array);
  }

  public abstract Dimension getMinSize();

  /** Lays out components. */
  public abstract void layout(LayoutMetrics metrics);

  public void render(Image output) {
    super.render(output);
    for(int layer=0;layer<=1;layer++) {
      for(Component child : children) {
        if (child.getLayer() != layer) continue;
        if (!child.isVisible()) continue;
        child.render(output);
      }
    }
  }

  private Component firstFocus() {
    for(Component child : children) {
      if (child.isFocusable()) {
        return child;
      }
      if (child instanceof Container) {
        Component c = ((Container)child).firstFocus();
        if (c != null) return c;
      }
    }
    return null;
  }

  private static boolean found;

  private Component nextFocus(Component focus) {
    for(Component child : children) {
      if (!found) {
        if (child == focus) {
          found = true;
        } else {
          if (child instanceof Container) {
            Component c = ((Container)child).nextFocus(focus);
            if (c != null) return c;
          }
        }
        continue;
      }
      if (child.isFocusable()) {
        return child;
      }
      if (child instanceof Container) {
        Component c = ((Container)child).nextFocus(focus);
        if (c != null) return c;
      }
    }
    return null;
  }

  private void nextFocus() {
    if (focus != null) {
      focus.onBlur();
      found = false;
      focus = nextFocus(focus);
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
    if (focus != null) {
      focus.keyTyped(ch);
    }
  }

  public void keyPressed(int key) {
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
    if (focus != null) {
      focus.keyReleased(key);
    }
  }

  public void mouseMove(int x, int y) {
    super.mouseMove(x, y);
    Point pt = getMousePosition();
    for(int layer=1;layer>=0;layer--) {
      for(Component child : children) {
        if (child.getLayer() != layer) continue;
        if (!child.isVisible()) continue;
        if (child.isInside(pt)) {
          child.mouseMove(x, y);
        }
      }
    }
  }

  public void mouseDown(int button) {
    Point pt = getMousePosition();
    super.mouseDown(button);
    for(int layer=1;layer>=0;layer--) {
      for(Component child : children) {
        if (child.getLayer() != layer) continue;
        if (!child.isVisible()) continue;
        if (child.isInside(pt)) {
          child.mouseDown(button);
          if (child.isConsumer()) {
            return;
          }
        }
      }
    }
  }

  public void mouseUp(int button) {
    Point pt = getMousePosition();
    super.mouseUp(button);
    for(int layer=1;layer>=0;layer--) {
      for(Component child : children) {
        if (child.getLayer() != layer) continue;
        if (!child.isVisible()) continue;
        if (child.isInside(pt)) {
          child.mouseUp(button);
          if (child.isConsumer()) return;
        }
      }
    }
  }

  public void mouseScroll(int dx, int dy) {
    Point pt = getMousePosition();
    super.mouseScroll(dx, dy);
    for(int layer=0;layer<=1;layer++) {
      for(Component child : children) {
        if (child.getLayer() != layer) continue;
        if (!child.isVisible()) continue;
        if (child.isInside(pt)) {
          child.mouseScroll(dx, dy);
        }
      }
    }
  }
}
