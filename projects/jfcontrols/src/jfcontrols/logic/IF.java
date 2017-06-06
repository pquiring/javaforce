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

public class IF extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "if";
  }

  public String getCode(int types[]) {
    return "if (enabled) {";
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
}
