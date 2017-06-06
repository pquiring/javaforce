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

public class BREAK extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Break";
  }

  public String getCode(int types[]) {
    return "if (enabled) break;";
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return -1;
  }
}
