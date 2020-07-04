package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class SLEEP extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Sleep";
  }

  public boolean execute(boolean enabled) {
    if (enabled) javaforce.JF.sleep(tags[1].getInt());
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.int32;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "ms";
      default: return null;
    }
  }
}
