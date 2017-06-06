package jfcontrols.panels;

/** NodeRoot
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.tags.*;

public class NodeRoot extends Node {
  public int fid;  //func id
  public int rid;  //rung id
  public boolean changed;
  public TextField comment;
  public TagsCache tags = new TagsCache();
  public NodeRoot(int fid, int rid) {
    this.root = this;
    this.type = 'r'; //root node
    this.fid = fid;
    this.rid = rid;
  }
  public String saveLogic(SQL sql) {
    JFLog.log("NodeRoot.saveLogic() " + this);
    int bid = 0;
    StringBuilder sb = new StringBuilder();
    Node node = next, child;
    while (node != null) {
      switch (node.type) {
        case 't':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
          sb.append(node.type);
          sb.append('|');
          break;
        case '#':
          sql.execute("insert into blocks (fid,rid,bid,name,tags) values (" + fid + "," + rid + "," + bid + ",'" + node.blk.getName().toUpperCase() + "'," + SQL.quote(node.getTags()) + ")");
          sb.append(Integer.toString(bid));
          bid++;
          sb.append('|');
          break;
      }
      node = node.next;
    }
    if (sb.length() == 0) {
      sb.append('h');
    }
    return sb.toString();
  }
  public boolean isValid(WebUIClient client) {
    int bid = 0;
    Node node = next, child;
    while (node != null) {
      switch (node.type) {
        case '#':
          //check all tags are valid
          int cnt = node.childs.size();
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.type == 'T') {
              TextField tf = (TextField)child.comp;
              String tag = tf.getText();
              if (tag.length() == 0 || tags.getTag(tag) == null) {
                Component focus = (Component)client.getProperty("focus");
                if (focus != null) {
                  focus.setBorder(false);
                  client.setProperty("focus", null);
                }
                Events.setFocus(tf);
                return false;
              }
            }
          }
          break;
      }
      node = node.next;
    }
    return true;
  }
}
