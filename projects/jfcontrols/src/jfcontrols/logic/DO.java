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

public class DO extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Do";
  }

  public String getCode(int types[]) {
    return "do {";
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
}
