package jfcontrols.logic;

/** Absolute
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class ABS extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Abs";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setFloat(Main.abs(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setDouble(Main.abs(tags[1].getDouble()));\r\n";
    if (types[1] == TagType.int64) return "if (enabled) tags[2].setLong(Main.abs(tags[1].getLong()));\r\n";
    return "if (enabled) tags[2].setInt(Main.abs(tags[1].getInt()));\r\n";
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "x";
      case 2: return "res";
      default: return null;
    }
  }
}
