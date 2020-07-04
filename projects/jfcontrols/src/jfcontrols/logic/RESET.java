package jfcontrols.logic;

/** Clears bit.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class RESET extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Reset";
  }

  public boolean execute(boolean enabled) {
    if (enabled) tags[1].setBoolean(false);
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
