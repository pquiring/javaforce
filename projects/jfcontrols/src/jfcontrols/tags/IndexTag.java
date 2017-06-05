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

  public TagBase getIndex(TagAddr ta) {
    JFLog.log("Error:IndexTag.getIndex() called");
    return null;
  }
}
