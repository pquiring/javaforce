package jfcontrols.logic;

import jfcontrols.tags.TagBase;

/** Continue
 *
 * Restart a looping block of code.
 *
 * See DO and WHILE for starting and ending a block of code.
 *
 * @author pquiring
 */

public class CONTINUE extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Continue";
  }

  public boolean doContinue;

  public boolean execute(boolean enabled) {
    doContinue = enabled;
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
    if (doContinue) {
      pos.rung = other.rung;
      pos.block = pos.rung.root;
    } else {
      super.moveNext(pos);
    }
  }
}
