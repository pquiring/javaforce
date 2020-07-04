package jfcontrols.panels;

/** NodeRoot
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.tags.*;
import jfcontrols.logic.*;
import jfcontrols.db.*;

public class NodeRoot extends Node {
  public int fid;  //func id
  public int rid;  //rung id
  public boolean changed;
  public TextField comment;
  public NodeRoot(int fid, int rid) {
    this.root = this;
    this.type = 'r'; //root node
    this.fid = fid;
    this.rid = rid;
  }
  public String saveLogic() {
    JFLog.log("NodeRoot.saveLogic() " + this);
    int bid = 0;
    StringBuilder sb = new StringBuilder();
    sb.append("h|");
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
          Database.addBlock(fid,rid,bid,node.blk.getName(),node.getTags());
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
    String strictValue = Database.getConfig("strict_tags");
    if (strictValue == null) strictValue = "false";
    boolean strict = strictValue.equals("true");
    while (node != null) {
      switch (node.type) {
        case '#':
          LogicBlock blk = node.blk;
          //check all tags are valid
          int cnt = node.childs.size();
          int tagidx = 0;
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.type == 'T') {
              tagidx++;
              TextField tf = (TextField)child.comp;
              try {
                String tagstr = tf.getText();
                if (tagstr.length() == 0) throw new Exception("no tag specified");
                if (!isTag(tagstr)) continue;  //immediate value
                TagBase tag = TagsService.getTag(tagstr);
                if (tag == null) throw new Exception("unknown tag");
                if (strict) {
                  int tagType = tag.getType();
                  int blkType = blk.getTagType(tagidx);
                  if (tagType != blkType) throw new Exception("tag mismatch");
                }
              } catch (Exception e) {
                Component focus = (Component)client.getProperty("focus");
                if (focus != null) {
                  focus.setBorder(false);
                  client.setProperty("focus", null);
                }
                Events.setError(tf);
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
  public boolean hasSolo() {
    Node node = this;
    while (node != null) {
      if (node.blk != null) {
        if (node.blk.isSolo()) return true;
      }
      node = node.next;
    }
    return false;
  }
  public boolean isEmpty() {
    Node node = this;
    while (node != null) {
      if (node.type != 'h' && node.type != 'r') {
        return false;
      }
      node = node.next;
    }
    return true;
  }
  public boolean hasLast() {
    Node node = this;
    while (node != null) {
      if (node.blk != null) {
        if (node.blk.isLast()) return true;
      }
      node = node.next;
    }
    return false;
  }
}
