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
import static jfcontrols.panels.Panels.layoutNodes;

public class Node {

  public char type;
  public String args; //if "#"
  public int x;
  public int y;
  public boolean closed;
  public Node prev;
  public Node next;
  public Node upper; //b,d only
  public Node lower; //a,c only
  public Node ref;
  public String bid;
  public Logic blk;
  public String tags;
  public Component comp;
  public NodeRoot root;
  public boolean highlight;  //possible fork dest

  public Node insertPreRef(Node ref, char type, int x, int y) {
    JFLog.log("insertPreRef:" + type);
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

  public Node insertPreNode(char type, int x, int y) {
    JFLog.log("insertPreNode:" + type);
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
    if (node.validFork()) {
      upper.lower = node;
    }
    return node;
  }

  public Node insertLinkLower(Node lower, char type, int x, int y) {
    JFLog.log("insertLinkLower:" + type);
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
    node.lower = lower;
//    lower.upper = node;
    return node;
  }

  public Node insertPreLinkUpper(Node upper, char type, int x, int y) {
    JFLog.log("insertPreLinkUpper:" + type);
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
    node.upper = upper;
//    upper.lower = node;
    return node;
  }

  public Node insertPreLinkLower(Node lower, char type, int x, int y) {
    JFLog.log("insertPreLinkLower:" + type);
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
    node.lower = lower;
//    lower.upper = node;
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

  /** Returns delta x to next node.
   * This may not be the actual width.
   * Only valid for major nodes (not ref nodes)
   */
  public int getDelta() {
    if (blk != null) {
      if (blk.isBlock()) {
        return 1;
      } else {
        return 2;
      }
    }
    return 1;
  }

  /** Returns actual width of node.
   * Only valid for major nodes (not ref nodes)
   */
  public int getWidth() {
    if (blk != null) {
      return 3;
    }
    return 1;
  }

  /** Returns actual height of node.
   * Only valid for major nodes (not ref nodes)
   */
  public int getHeight() {
    if (blk != null) {
      if (blk.isBlock()) {
        return 3 + blk.getTagsCount();
      } else {
        if (blk.getTagsCount() == 1) return 2;
      }
    }
    return 1;
  }

  public boolean validFork() {
    if (ref != null) return false;
    switch (type) {
      case 'h':
      case 't':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
        return true;
    }
    return false;
  }

  public boolean endFork() {
    switch (type) {
      case 't':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
        return true;
    }
    return false;
  }

  public void setHighlight(boolean state) {
    if (state) comp.setBorderColor("#0f0");
    comp.setBorder(state);
    highlight = state;
  }

  public void forkSource(WebUIClient client) {
    Node node = this;
    boolean valid = false;
    while (!node.validFork()) {
      if (node.type == 'v') {
        node = node.upper;
      } else {
        node = node.prev;
      }
      while (node != null && node.ref != null) {
        node = node.prev;
      }
    }
    if (node == null) return;
    //highlight possible destinations
    Node src = node;
    node = node.next;
    if (node != null && node.ref != null) node = node.ref;
    int cnt = 0;
    while (node != null) {
      if (node.validFork()) {
        node.setHighlight(true);
        cnt++;
      }
      node = node.next;
      if (node != null && node.ref != null) {
        node = node.ref;
      }
      if (node != null && node.endFork()) {
        break;
      }
    }
    if (cnt == 0) {
      JFLog.log("Fork:no possible destinations");
      return;
    }
    JFLog.log("Fork:select destination");
    //TODO : update "fork" button to indicate a cancel function
    client.setProperty("fork", src);
  }

  public void forkDest(WebUIClient client, Table table, Node src) {
    //src = fork source
    //dest = fork destination
    Node dest = this, node;
    if (dest.ref != null) dest = dest.ref;
    if (!dest.highlight) return;
    //fork it!
    JFLog.log("pre logic=" + root.saveLogic(true));
    src = src.insertNode('t', src.x + 1, src.y);
    src.insertNode('h', src.x + 1, src.y);
    dest = dest.insertNode('t', src.x + 1, src.y);
    dest.insertNode('h', src.x + 1, src.y);
    dest.insertLinkUpper(dest, 'd', dest.x, dest.y + 1);
    dest.insertLinkUpper(src, 'c', src.x, src.y + 1);
    client.setProperty("fork", null);
    JFLog.log("pst logic=" + root.saveLogic(true));
    node = dest.root;
    //clear all highlighted destinations
    while (node != null) {
      if (node.highlight) node.setHighlight(false);
      node = node.next;
    }
    try {
      layoutNodes(dest.root, table);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void forkCancel(WebUIClient client) {
    JFLog.log("Fork:cancel");
    client.setProperty("fork", null);
    Node node = root;
    JFLog.log("root=" + node);
    //clear all highlighted destinations
    while (node != null) {
      if (node.highlight) node.setHighlight(false);
      node = node.next;
    }
  }

  public int adjustX(int x) {
    //TODO : may need to move over to right for branches below this node
    return x;
  }
}
