package javaforce.webui;

/** WebUI TreeModelEvent
 *
 * @author pquiring
 */

public class TreeModelEvent {
  public TreePath path;
  public int[] indices;
  public TreePath getTreePath() {return path;}
  public int[] getChildIndices() {return indices;}
}
