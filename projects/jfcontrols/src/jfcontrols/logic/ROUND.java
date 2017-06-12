package jfcontrols.logic;

/** Round float -> int types
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class ROUND extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Round";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setInt(Math.round(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setLong(Math.round(tags[1].getDouble()));\r\n";
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

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "x";
      case 2: return "res";
      default: return null;
    }
  }
}
