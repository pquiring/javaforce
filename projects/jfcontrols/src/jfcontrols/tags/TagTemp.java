package jfcontrols.tags;

/** Tag with temp value
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class TagTemp extends TagBase {
  private String value;

  public TagTemp(String initValue) {
    super(TagType.unknown, false, false);
    this.value = initValue;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public TagBase getIndex(TagAddr ta) {
    JFLog.log("Error:TagTemp.getIndex() called");
    return null;
  }
}
