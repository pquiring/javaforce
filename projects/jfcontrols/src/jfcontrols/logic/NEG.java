package jfcontrols.logic;

/** Negative Edge detection
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class NEG extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "pos";
  }

  public boolean execute(boolean enabled) {
    if (enabled) {
      if (!tags[1].getBoolean()) tags[1].setBoolean(true);
      enabled = false;
    } else {
      if (tags[1].getBoolean()) {tags[1].setBoolean(false); enabled = true;}
    }
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
