package jfcontrols.logic;

/** Ceil
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class CEIL extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Ceil";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setInt((int)Math.ceil(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setLong((long)Math.ceil(tags[1].getDouble()));\r\n";
    return null;  //wrong tag type
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyfloat;
      case 2: return TagType.any;
    }
    return TagType.unknown;
  }
}
