package jfcontrols.logic;

/** Wire -
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class Wire extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "";
  }

  public boolean execute(boolean enabled) {
    return enabled;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }
}
