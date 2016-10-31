package javaforce.webui;

/** Tree
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class Tree extends ScrollPanel implements Click {
  public Tree() {
    removeClass("column");
    addClass("tree");
  }
  private TreeNode root = new TreeNode();
  public void init() {
    rebuild();
    super.init();
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + "'>");
    sb.append(innerhtml());
    sb.append("</div>");
    return sb.toString();
  }
  public String innerhtml() {
    StringBuffer sb = new StringBuffer();
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    return sb.toString();
  }
  public void setRootNode(TreeNode newRoot) {
    root = newRoot;
    rebuild();
    client.sendEvent("sethtml", id, new String[] {"html=" + innerhtml()});
  }
  public TreeNode getRootNode() {
    return root;
  }
  private void addNode(TreeNode node, int offset) {
    Block row = new Block();
    row.setClass("treerow");
    row.display = "flex";
    add(row);
    row.setProperty("node", node);
    row.addClickListener(this);
    for(int a=0;a<offset;a++) {
      Block b = new Block();
      b.setClass("treeindex");
      row.add(b);
    }
    if (node.hasChildren() && !node.leaf) {
      Block b = new Block();
      if (node.opened) {
        b.setClass("treeopened");
        row.add(b);
      } else {
        b.setClass("treeclosed");
        row.add(b);
      }
      b.addEvent("onclick", "onClick(event," + row.id + ");");
      b.addClickListener(this);
      row.setProperty("open_close", b);
    } else {
      Block b = new Block();
      b.setClass("treeleaf");
      row.add(b);
    }
    Label l = new Label(node.toString());
    l.setClass("treelabel");
    l.addEvent("onclick", "onClick(event," + row.id + ");");
    l.addClickListener(this);
    row.add(l);
    int cnt = node.getChildrenCount();
    offset++;
    for(int a=0;a<cnt;a++) {
      addNode(node.getChildAt(a), offset);
    }
  }
  private void rebuild() {
    removeAll();
    addNode(root, 0);
  }
  private TreeEventClick handler;
  public void addEventHandler(TreeEventClick handler) {
    this.handler = handler;
  }
  private Block getRow(TreeNode node) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Block row = (Block)get(a);
      if (row.getProperty("node") == node) {
        return row;
      }
    }
    return null;
  }
  private void setNodeVisible(TreeNode node, boolean state) {
    Block row = getRow(node);
    row.setVisible(state);
    int cnt = node.getChildrenCount();
    for(int a=0;a<cnt;a++) {
      setNodeVisible(node.getChildAt(a), state);
    }
  }
  private void setNodeChildrenVisible(TreeNode node, boolean state) {
    int cnt = node.getChildrenCount();
    for(int a=0;a<cnt;a++) {
      setNodeVisible(node.getChildAt(a), state);
    }
    Block row = (Block)getRow(node);
    Block oc = (Block)row.getProperty("open_close");
    oc.setClass(state ? "treeopened" : "treeclosed");
  }
  public void onClick(MouseEvent e, Component c) {
    TreeNode node = (TreeNode)c.getProperty("node");
    if (node == null) return;
    if (!node.hasChildren() || node.leaf) {
      //trigger click event
      if (handler != null) handler.nodeClick(node);
    } else {
      //open or close node
      node.opened = !node.opened;
      setNodeChildrenVisible(node, node.opened);
    }
  }
}
