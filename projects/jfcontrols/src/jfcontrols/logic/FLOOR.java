package jfcontrols.logic;

/** Floor
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class FLOOR extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Floor";
  }

  public boolean execute(boolean enabled) {
    switch (tags[1].type) {
      case TagType.float32: if (enabled) tags[2].setInt((int)Math.floor(tags[1].getFloat())); break;
      case TagType.float64: if (enabled) tags[2].setLong((long)Math.floor(tags[1].getDouble())); break;
    }
    return enabled;
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyfloat;
      case 2: return TagType.any;
    }
    return TagType.unknown;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "x";
      case 2: return "res";
      default: return null;
    }
  }
}
