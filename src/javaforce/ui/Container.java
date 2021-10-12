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

  /** Lays out components in a default flow pattern. */
  public void layout(LayoutMetrics metrics) {
    int max_y = 0;
    int init_x = metrics.pos.x;
    int init_y = metrics.pos.y;
    int width = metrics.size.width;
    int height = metrics.size.height;
    for(Component child : children) {
      Dimension size = child.getMinSize();
      if (metrics.pos.x > init_x && metrics.pos.x + size.width > metrics.size.width) {
        metrics.pos.x = init_x;
        metrics.pos.y += max_y;
        max_y = 0;
      }
      child.pos.x = metrics.pos.x;
      child.pos.y = metrics.pos.y;
      metrics.pos.x += child.pos.x;
      if (size.height > max_y) {
        max_y = size.height;
      }
      if (child instanceof Container) {
        int org_width = metrics.size.width;
        int org_height = metrics.size.height;
        int org_x = metrics.pos.x;
        int org_y = metrics.pos.y;
        metrics.size.width = width - metrics.pos.x;
        metrics.size.height = size.height;
        child.layout(metrics);
        metrics.size.width = org_width;
        metrics.size.height = org_height;
        metrics.pos.x = org_x;
        metrics.pos.y = org_y;
      }
    }
  }

  public void render(Image output) {
    for(Component child : children) {
      child.render(output);
    }
    super.render(output);
  }
}
