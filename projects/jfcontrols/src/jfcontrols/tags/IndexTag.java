package jfcontrols.tags;

/** Index Tag @bbb
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class IndexTag extends TagBase {
  private IndexTags tags;
  private int idx;

  public IndexTag(IndexTags tags, int idx) {
    super(TagType.int32, false, false);
    this.tags = tags;
    this.idx = idx;
  }

  public String getValue() {
    return Integer.toString(tags.getIndex(idx));
  }

  public void setValue(String value) {
    tags.setIndex(idx, Integer.valueOf(value));
  }

  public TagBase getIndex(int idx) {
    JFLog.log("Error:IndexTag.getIndex() called");
    return null;
  }

  public TagBase getMember(int idx) {
    JFLog.log("Error:IndexTag.getMember() called");
    return null;
  }

  public int getMember(String name) {
    JFLog.log("Error:IndexTag.getMember() called");
    return -1;
  }

  public int getTagID() {
    return -1;
  }

  public int getIndex() {
    return idx;
  }

  public boolean isMember() {
    return false;
  }

  public int getMember() {
    return -1;
  }

  public int getMemberIndex() {
    return -1;
  }
}
