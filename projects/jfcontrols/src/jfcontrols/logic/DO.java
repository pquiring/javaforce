package jfcontrols.logic;

/** DO
 *
 * Starts a looping block of code.
 *
 * See DO_END for ending block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.TagBase;

public class DO extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Do";
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

  public boolean isSolo() {
    return true;
  }

  public boolean isFlowControl() {
    return true;
  }
}
