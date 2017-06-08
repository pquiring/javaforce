package jfcontrols.logic;

/** Return from function.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class RET extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "Return";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) return tags[1].getBoolean();\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }

  public boolean isLast() {
    return true;
  }
}
