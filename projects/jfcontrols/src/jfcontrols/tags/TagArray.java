package jfcontrols.tags;

/** Array index into tag.
 *
 * @author pquiring
 */

import javaforce.*;

public class TagArray extends MonitoredTag {
  private TagBaseArray tag;
  private TagAddr addr;
  public TagArray(TagBase tag, TagBaseArray base, TagAddr addr) {
    super(tag.type, tag.unsigned, tag.array);
    this.tag = base;
  }

  public String getValue() {
    return tag.getValue(addr);
  }

  public void setValue(String value) {
    tag.setValue(addr, value);
  }

  public TagBase getIndex(TagAddr ta) {
    JFLog.log("Error:ArrayTag.getIndex() called");
    return null;
  }

  public TagAddr getAddr() {
    return addr;
  }

  public void updateRead(SQL sql) {
  }

  public void updateWrite(SQL sql) {
  }
}
