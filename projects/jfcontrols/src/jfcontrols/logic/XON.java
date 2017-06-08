package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class XON extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "xon";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "enabled &= tags[1].getBoolean();\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
