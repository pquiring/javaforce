package jfcontrols.logic;

/** Break
 *
 * Break out of a looping block of code.
 *
 * See DO and WHILE for starting and ending a block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class BREAK extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Break";
  }

  public boolean doBreak;

  public boolean execute(boolean enabled) {
    doBreak = enabled;
    return enabled;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return -1;
  }

  public boolean isLast() {
    return true;
  }

  public void moveNext(LogicPos pos) throws Exception {
    if (doBreak) {
      pos.rung = other.other.rung.next;
      pos.block = pos.rung.root;
    } else {
      super.moveNext(pos);
    }
  }
}
