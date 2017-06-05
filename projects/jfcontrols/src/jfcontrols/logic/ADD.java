package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class ADD extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Add";
  }

  public String getCode(int types[]) {
    if (types[3] == TagType.float32) return "if (enabled) tags[3].setFloat(tags[1].getFloat() + tags[2].getFloat());\r\n";
    if (types[3] == TagType.float64) return "if (enabled) tags[3].setDouble(tags[1].getDouble() + tags[2].getDouble());\r\n";
    if (types[3] == TagType.int64) return "if (enabled) tags[3].setLong(tags[1].getLong() + tags[2].getLong());\r\n";
    return "if (enabled) tags[3].setInt(tags[1].getInt() + tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }
}
