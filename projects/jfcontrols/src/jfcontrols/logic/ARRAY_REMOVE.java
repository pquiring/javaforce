package jfcontrols.logic;

import javaforce.controls.*;

/**  Array remove
 *
 */

public class ARRAY_REMOVE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ArrayRemove";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) arrayremove(tags);";
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyarray;
      case 2: return TagType.anyint;
    }
    return TagType.unknown;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "array";
      case 2: return "off";
      default: return null;
    }
  }
}
