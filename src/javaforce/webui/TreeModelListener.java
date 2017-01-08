package javaforce.webui;

/** WebUI TreeModelListener
 *
 * @author pquiring
 */

public interface TreeModelListener {
  public void treeNodesChanged(TreeModelEvent e);
  public void treeNodesInserted(TreeModelEvent e);
  public void treeNodesRemoved(TreeModelEvent e);
  public void treeStructureChanged(TreeModelEvent e);
}
