package javaforce.webui;

/** WebUI TreeModel
 *
 * @author pquiring
 */

public class TreeModel {
  private TreeNode root;
  private TreeModelListener listener;

  public TreeModel(TreeNode root) {
    this.root = root;
  }
  public TreeNode getRoot() {
    return root;
  }
  public void setRoot(TreeNode root) {
    this.root = root;
  }
  public void addTreeModelListener(TreeModelListener listener) {
    this.listener = listener;
  }
  public void nodeChanged(TreeNode node) {}
  public void removeNodeFromParent(TreeNode node) {}
  public void insertNodeInto(TreeNode child, TreeNode parent, int idx) {}
}
