package jfcontrols.logic;

import javaforce.controls.*;

/**  Array length
 *
 */

public class ARRAY_LENGTH extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ArrayLength";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) arraylength(tags);";
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
      case 2: return "len";
      default: return null;
    }
  }
}
