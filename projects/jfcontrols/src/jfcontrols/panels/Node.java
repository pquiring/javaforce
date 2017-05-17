package jfcontrols.panels;

/** Node */

import java.util.ArrayList;
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

public class Node {

  public char type;
  public String args; //if "b"
  public int rid;
  public int x;
  public int y;
  public boolean closed;
  public Node prev;
  public Node next;
  public Node upper; //b,d only
  //b,d only
  public String bid;
  public Logic blk;
  public String tags;
  public ArrayList<NodeRef> refs = new ArrayList<NodeRef>();

  public Node(Node prev, int rung, char type, int x, int y) {
    if (prev != null) {
      prev.next = this;
    }
    this.prev = prev;
    this.rid = rung;
    this.type = type;
    this.x = x;
    this.y = y;
  }

  public Node(Node prev, Node upper, int rung, char type, int x, int y) {
    this.upper = upper;
    if (prev != null) {
      prev.next = this;
    }
    this.prev = prev;
    this.rid = rung;
    this.type = type;
    this.x = x;
    this.y = y;
  }

  public Node(Node prev, int rung, char type, int x, int y, String bid, Logic blk, String tags) {
    if (prev != null) {
      prev.next = this;
    }
    this.prev = prev;
    this.rid = rung;
    this.type = type;
    this.x = x;
    this.y = y;
    this.bid = bid;
    this.blk = blk;
    this.tags = tags;
  }

  public void close() {
    closed = true;
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
          return node;
        }
      }
    }
    return null;
  }

}
