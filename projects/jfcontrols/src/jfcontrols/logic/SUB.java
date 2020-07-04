package jfcontrols.logic;

/** Sub
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class SUB extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Sub";
  }

  public boolean execute(boolean enabled) {
    switch (tags[3].type) {
      case TagType.float32: if (enabled) tags[3].setFloat(tags[1].getFloat() - tags[2].getFloat()); break;
      case TagType.float64: if (enabled) tags[3].setDouble(tags[1].getDouble() - tags[2].getDouble()); break;
      case TagType.int64: if (enabled) tags[3].setLong(tags[1].getLong() - tags[2].getLong()); break;
      default: if (enabled) tags[3].setInt(tags[1].getInt() - tags[2].getInt()); break;
    }
    return enabled;
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
