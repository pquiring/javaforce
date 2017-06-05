package jfcontrols.tags;

/** Tag with temp value
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class TagTemp extends TagBase {
  private String value;

  public TagTemp(String initValue) {
    super(TagType.unknown, false, false);
    this.value = initValue;
  }

  public String getValue(TagAddr addr) {
    return value;
  }

  public void setValue(TagAddr addr, String value) {
    this.value = value;
  }
}
