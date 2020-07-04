package jfcontrols.logic;

/** End While
 *
 * Ends a looping block of code and jumps to while_begin rung.
 *
 * See While_begin for starting block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class WHILE_END extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "}";
  }

  public boolean doWhileEnd;

  public boolean execute(boolean enabled) {
    doWhileEnd = enabled;
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

  public boolean isFlowControl() {
    return true;
  }

  public boolean canClose(String name) {
    return name.equals("IF") || name.equals("WHILE");
  }

  public void moveNext(LogicPos pos) throws Exception {
    if (doWhileEnd) {
      pos.rung = other.rung;
      pos.block = other.rung.root;
    } else {
      super.moveNext(pos);
    }
  }
}
