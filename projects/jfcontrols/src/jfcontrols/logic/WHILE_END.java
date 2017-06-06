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

public class WHILE_END extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "}";
  }

  public String getCode(int types[]) {
    return "}";
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
