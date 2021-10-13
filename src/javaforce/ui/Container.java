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
    for(Component child : children) {
      child.render(output);
    }
    super.render(output);
  }
}
