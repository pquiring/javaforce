package jfcontrols.logic;

/** Square Root
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SQRT extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "SQRT";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "if (enabled) tags[2].setFloat((float)Math.sqrt(tags[1].getFloat()));\r\n";
    if (types[1] == TagType.float64) return "if (enabled) tags[2].setDouble(Math.sqrt(tags[1].getDouble()));\r\n";
    return null;  //wrong tag type
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.anyfloat;
  }
}
