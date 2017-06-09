package jfcontrols.logic;

/** Continue
 *
 * Restart a looping block of code.
 *
 * See DO and WHILE for starting and ending a block of code.
 *
 * @author pquiring
 */

public class CONTINUE extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Continue";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) continue;";
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
