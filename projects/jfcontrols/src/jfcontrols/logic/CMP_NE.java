package jfcontrols.logic;

/** Compare Not Equal
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class CMP_NE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Compare !=";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.float32) return "enabled &= (tags[1].getFloat() != tags[2].getFloat());\r\n";
    if (types[1] == TagType.float64) return "enabled &= (tags[1].getDouble() !=  tags[2].getDouble());\r\n";
    if (types[1] == TagType.int64) return "enabled &= (tags[1].getLong() != tags[2].getLong());\r\n";
    return "enabled &= (tags[1].getInt() != tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }
}
