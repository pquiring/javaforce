package jfcontrols.tags;

/** Index Tag @bbb
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class IndexTag extends TagBase {
  private IndexTags tags;
  private int idx;

  public IndexTag(IndexTags tags, int idx) {
    super(0, TagType.int32, false, false);
    this.tags = tags;
    this.idx = idx;
  }

  public String getValue(TagAddr addr) {
    return Integer.toString(tags.getIndex(idx));
  }

  public void setValue(TagAddr addr, String value) {
    tags.setIndex(idx, Integer.valueOf(value));
  }
}
