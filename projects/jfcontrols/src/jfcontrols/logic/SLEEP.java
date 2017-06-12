package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SLEEP extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Sleep";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) javaforce.JF.sleep(tags[1].getInt());\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.int32;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "ms";
      default: return null;
    }
  }
}
