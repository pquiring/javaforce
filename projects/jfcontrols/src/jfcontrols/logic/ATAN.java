package jfcontrols.logic;

/** Arc Tangent
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class ATAN extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ATAN";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setFloat((float)Math.atan(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setDouble(Math.atan(tags[1].getDouble()));\r\n";
    return null;  //wrong tag type
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.anyfloat;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "x";
      case 2: return "res";
      default: return null;
    }
  }
}
