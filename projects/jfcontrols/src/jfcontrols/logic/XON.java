package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class XON extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "xon";
  }

  public boolean execute(boolean enabled) {
    return enabled & tags[1].getBoolean();
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
