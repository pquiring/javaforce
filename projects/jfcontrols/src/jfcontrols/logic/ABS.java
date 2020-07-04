package jfcontrols.logic;

/** Absolute
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class ABS extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Abs";
  }

  public boolean execute(boolean enabled) {
    switch (tags[1].type) {
      case TagType.float32: if (enabled) tags[2].setFloat(Math.abs(tags[1].getFloat())); break;
      case TagType.float64: if (enabled) tags[2].setDouble(Math.abs(tags[1].getDouble())); break;
      case TagType.int64: if (enabled) tags[2].setLong(Math.abs(tags[1].getLong())); break;
      default: if (enabled) tags[2].setInt(Math.abs(tags[1].getInt())); break;
    }
    return enabled;
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
