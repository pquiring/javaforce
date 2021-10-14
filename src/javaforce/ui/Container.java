package javaforce.ui;

/** Container - contains other Components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Container extends Component {
  public ArrayList<Component> children;
  public Dimension minSize = new Dimension();

  public Container() {
    children = new ArrayList<>();
  }

  public void add(Component child) {
    children.add(child);
  }

  public void remove(Component child) {
    children.remove(child);
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

  public void keyTyped(char ch) {
  }

  public void keyPressed(int key) {
  }

  public void keyReleased(int key) {
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
