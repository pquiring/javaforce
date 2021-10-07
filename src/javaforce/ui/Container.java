package javaforce.ui;

/** Container - contains other Components.
 *
 * @author pquiring
 */

import java.util.*;

public class Container extends Component {
  private ArrayList<Component> children;

  public void add(Component child) {
    children.add(child);
  }

  public void remove(Component child) {
    children.remove(child);
  }

  public void layout(LayoutMetrics metrics) {
    //TODO
  }
}
