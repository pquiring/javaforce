package jfcontrols.logic;

/** IF
 *
 * Starts a block of code if condition is true.
 *
 * See IF_END for ending block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class IF extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "if";
  }

  public boolean enabled;

  public boolean execute(boolean enabled) {
    this.enabled = enabled;
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

  public void moveNext(LogicPos pos) {
    if (enabled) {
      pos.block = next;
    } else {
      pos.rung = other.rung.next;
      if (pos.rung != null) {
        pos.block = pos.rung.root;
      } else {
        pos.block = null;
      }
    }
  }
}
