package jfcontrols.logic;

/** While
 *
 * Ends a looping block of code and jumps to last Do if enabled.
 *
 * See Do for starting block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class WHILE extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getName() {
    return "While";
  }

  public String getCode(int types[]) {
    return "} while(enabled);";
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return -1;
  }
}
