package jfcontrols.panels;

/** Node */

import java.util.ArrayList;
import javaforce.JFLog;
import jfcontrols.logic.Logic;

/*
Ladder Logic Notation
---------------------
hth#hth
 v   v
 ah#hb
 v   v
 ch#hd
block(t#,c#t#,i#,d#,a#,...) - call block
c# = controller id # (optional, default = 0)
t# = tag id #
i# = immediate id #
d# = digital port
a# = analog port
f# = function id #
Example blocks:
func(c#f#,args...) - call another function
xon(t) - examine bit on
xoff(t) - example bit off
coil(t) - coil output
set(t) - set bit
reset(t) - reset bit
Example Logic:
xon(c1t1)|f(t2,t3)|t|xon(t4)|t|c,xon(t5)|d|coil(t6)
*/

import javaforce.webui.*;

public class Node {

  public char type;
  public String args; //if "#"
  public int x;
  public int y;
  public boolean closed;
  public Node prev;
  public Node next;
  public Node upper; //b,d only
  public Node ref;
  public String bid;
  public Logic blk;
  public String tags;
  public Component comp;
  public NodeRoot root;

  public Node insertRef(Node ref, char type, int x, int y) {
    JFLog.log("insertRef:" + type);
    Node node = new Node();
    node.root = root;
    //insert BEFORE this node
    node.prev = prev;
    node.next = this;
    if (prev != null) prev.next = node;
    prev = node;

    node.type = type;
    node.x = x;
    node.y = y;
    node.ref = ref;
    return node;
  }

  public Node insertPre(char type, int x, int y) {
    JFLog.log("insertPre:" + type);
    Node node = new Node();
    node.root = root;
    //insert BEFORE this node
    node.prev = prev;
    node.next = this;
    if (prev != null) prev.next = node;
    prev = node;

    node.type = type;
    node.x = x;
    node.y = y;
    return node;
  }

  public Node insertNode(char type, int x, int y) {
    JFLog.log("insertNode:" + type);
    Node node = new Node();
    node.root = root;
    //insert AFTER this node
    node.prev = this;
    node.next = next;
    if (next != null) next.prev = node;
    next = node;

    node.type = type;
    node.x = x;
    node.y = y;
    return node;
  }

  public Node insertLogic(char type, int x, int y, String bid, Logic blk, String tags) {
    JFLog.log("insertLogic:" + type);
    Node node = new Node();
    node.root = root;
    //insert AFTER this node
    node.prev = this;
    node.next = next;
    if (next != null) next.prev = node;
    next = node;

    node.type = type;
    node.x = x;
    node.y = y;
    node.bid = bid;
    node.blk = blk;
    node.tags = tags;
    return node;
  }

  public Node insertLinkUpper(Node upper, char type, int x, int y) {
    JFLog.log("insertLinkUpper:" + type);
    Node node = new Node();
    node.root = root;
    //insert AFTER this node
    node.prev = this;
    node.next = next;
    if (next != null) next.prev = node;
    next = node;

    node.type = type;
    node.x = x;
    node.y = y;
    node.upper = upper;
    return node;
  }

  public static Node findFirstOpenNode(ArrayList<Node> stack, String types) {
    int cnt = stack.size();
    char[] ca = types.toCharArray();
    for (int p = 0; p < cnt; p++) {
      Node node = stack.get(p);
      if (node.closed) {
        continue;
      }
      for (int c = 0; c < ca.length; c++) {
        if (node.type == ca[c]) {
          node.closed = true;
          return node;
        }
      }
    }
    return null;
  }

  public static Node findLastOpenNode(ArrayList<Node> stack, String types) {
    int cnt = stack.size();
    char[] ca = types.toCharArray();
    for (int p = cnt - 1; p >= 0; p--) {
      Node node = stack.get(p);
      if (node.closed) {
        continue;
      }
      for (int c = 0; c < ca.length; c++) {
        if (node.type == ca[c]) {
          node.closed = true;
          return node;
        }
      }
    }
    return null;
  }

  public int getWidth() {
    if (blk != null) {
      if (blk.isBlock()) {
        return 5;
      } else {
        return 3;
      }
    }
    return 1;
  }

  public int getHeight() {
    if (blk != null) {
      if (blk.isBlock()) {
        return 3 + blk.getTagsCount();
      } else {
        return 1 + blk.getTagsCount();
      }
    }
    return 1;
  }
}
