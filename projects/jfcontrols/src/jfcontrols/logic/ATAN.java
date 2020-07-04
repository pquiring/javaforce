package jfcontrols.logic;

/** Arc Tangent
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class ATAN extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ATAN";
  }

  public boolean execute(boolean enabled) {
    switch (tags[1].type) {
      case TagType.float32: if (enabled) tags[2].setFloat((float)Math.atan(tags[1].getFloat())); break;
      case TagType.float64: if (enabled) tags[2].setDouble(Math.atan(tags[1].getDouble())); break;
    }
    return enabled;
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
