package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class COIL extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Coil";
  }

  public boolean execute(boolean enabled) {
    tags[1].setBoolean(enabled);
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
