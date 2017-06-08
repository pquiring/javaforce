package jfcontrols.logic;

/** Cosine
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class COS extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "COS";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setFloat((float)Math.cos(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setDouble(Math.cos(tags[1].getDouble()));\r\n";
    return null;  //wrong tag type
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.anyfloat;
  }
}
