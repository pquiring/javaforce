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

  public TagBase getIndex(int idx) {
    JFLog.log("Error:TagTemp.getIndex() called");
    return null;
  }

  public TagBase getMember(int idx) {
    JFLog.log("Error:TagTemp.getMember() called");
    return null;
  }

  public int getMember(String name) {
    JFLog.log("Error:TagTemp.getMember() called");
    return -1;
  }

  public int getTagID() {
    return -1;
  }

  public int getIndex() {
    return 0;
  }

  public boolean isMember() {
    return false;
  }

  public int getMember() {
    return 0;
  }

  public int getMemberIndex() {
    return 0;
  }
}
