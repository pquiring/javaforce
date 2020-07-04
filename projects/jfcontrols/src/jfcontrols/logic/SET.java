package jfcontrols.logic;

/** Sets bit.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class SET extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "set";
  }

  public boolean execute(boolean enabled) {
    if (enabled) tags[1].setBoolean(true);
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
