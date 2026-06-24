package javaforce.webui;

import java.util.*;

/** WebUI TreePath
 *
 * @author pquiring
 */

public class TreePath {
  private ArrayList<TreeNode> path = new ArrayList<TreeNode>();
  public TreeNode[] getPath() {
    return path.toArray(new TreeNode[path.size()]);
  }
  public TreeNode getLastPathComponent() {
    return path.get(path.size() - 1);
  }
}
