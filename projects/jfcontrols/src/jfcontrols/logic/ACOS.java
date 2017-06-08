package jfcontrols.logic;

/** Arc Cosine
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class ACOS extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ACOS";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setFloat((float)Math.acos(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setDouble(Math.acos(tags[1].getDouble()));\r\n";
    return null;  //wrong tag type
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.anyfloat;
  }
}
