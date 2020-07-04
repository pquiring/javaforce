package jfcontrols.logic;

/** While (cond) {
 *
 * Starts a looping block of code.
 *
 * See End_While for ending block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class WHILE extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "while () {";
  }

  public boolean execute(boolean enabled) {
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

  public LogicBlock getNext() {
    return next;
  }
}
