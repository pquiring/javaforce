package jfcontrols.logic;

import javaforce.controls.*;

/**  Array copy
 *
 *   src and dst may overlap.
 *
 */

public class ARRAY_COPY extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ArrayCopy";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) arraycopy(tags);";
  }

  public int getTagsCount() {
    return 5;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyarray;
      case 2: return TagType.anyint;
      case 3: return TagType.anyarray;
      case 4: return TagType.anyint;
      case 5: return TagType.anyint;
    }
    return TagType.unknown;
  }
}
