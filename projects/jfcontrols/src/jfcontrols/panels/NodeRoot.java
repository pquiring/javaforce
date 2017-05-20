package jfcontrols.panels;

/** NodeRoot
 *
 * @author pquiring
 */

import javaforce.*;

public class NodeRoot extends Node {
  public int fid;  //func id
  public int rid;  //rung id
  public boolean changed;
  public NodeRoot(int fid, int rid) {
    this.root = this;
    this.type = 'r'; //root node
    this.fid = fid;
    this.rid = rid;
  }
  public String saveLogic(SQL sql) {
    int bid = 0;
    StringBuilder sb = new StringBuilder();
    Node node = next;
    while (node != null) {
      switch (node.type) {
        case 't':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
          sb.append(node.type);
          break;
        case '#':
          sql.execute("insert into blocks (fid,rid,bid,name,tags) values (" + fid + "," + rid + "," + bid + ",'" + node.blk.getName() + "'," + SQL.quote(node.getTags()) + ")");
          sb.append(Integer.toString(bid));
          bid++;
          break;
      }
      node = node.next;
    }
    return sb.toString();
  }
}
