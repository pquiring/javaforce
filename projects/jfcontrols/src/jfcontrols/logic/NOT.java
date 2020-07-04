package jfcontrols.logic;

/** Not bit logic.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class NOT extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "not";
  }

  public boolean execute(boolean enabled) {
    return !enabled;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.unknown;
  }
}
