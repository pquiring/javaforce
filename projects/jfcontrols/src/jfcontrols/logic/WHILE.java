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

public class WHILE extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "while () {";
  }

  public String getPreCode() {
    return "while (true) {";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (!enabled) break;";
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
}
