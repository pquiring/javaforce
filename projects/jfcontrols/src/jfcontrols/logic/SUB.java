package jfcontrols.logic;

/** Sub
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SUB extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Sub";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[3] == TagType.int64) return "if (enabled) tags[3].setLong(tags[1].getLong() - tags[2].getLong());\r\n";
    if (types[3] == TagType.float64) return "if (enabled) tags[3].setDouble(tags[1].getDouble() - tags[2].getDouble());\r\n";
    if (types[3] == TagType.float32) return "if (enabled) tags[3].setFloat(tags[1].getFloat() - tags[2].getFloat());\r\n";
    return "if (enabled) tags[3].setInt(tags[1].getInt() - tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "x";
      case 2: return "y";
      case 3: return "res";
      default: return null;
    }
  }
}
