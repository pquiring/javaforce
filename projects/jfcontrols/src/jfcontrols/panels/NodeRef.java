package jfcontrols.panels;

/** NodeRef
 *
 * @author pquiring
 */

public class NodeRef {

  public Node ref;
  public int x;
  public int y;

  public NodeRef(Node node, int x, int y) {
    ref = node;
    this.x = x;
    this.y = y;
    node.refs.add(this);
  }

}
