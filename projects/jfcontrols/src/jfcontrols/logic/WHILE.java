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

  public String getCode(int types[]) {
    return "while(enabled) {";
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
