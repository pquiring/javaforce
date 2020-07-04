package jfcontrols.logic;

/** Do End
 *
 * } while
 *
 * Ends a looping block of code and jumps to last Do if enabled.
 *
 * See Do for starting block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class DO_END extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "} while()";
  }

  public boolean doDoEnd;

  public boolean execute(boolean enabled) {
    doDoEnd = enabled;
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
    return name.equals("DO");
  }

  public void moveNext(LogicPos pos) throws Exception {
    if (doDoEnd) {
      pos.rung = other.rung;
      pos.block = other.rung.root;
    } else {
      super.moveNext(pos);
    }
  }
}
