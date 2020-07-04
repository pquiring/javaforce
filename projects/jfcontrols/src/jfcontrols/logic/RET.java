package jfcontrols.logic;

/** Return from function.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class RET extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Return";
  }

  public boolean doReturn;

  public boolean execute(boolean enabled) {
    doReturn = enabled;
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }

  public boolean isLast() {
    return true;
  }

  public LogicBlock getNext() {
    if (doReturn) {
      return null;
    }
    return next;
  }
}
