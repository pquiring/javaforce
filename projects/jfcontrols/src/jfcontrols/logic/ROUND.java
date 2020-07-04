package jfcontrols.logic;

/** Round float -> int types
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class ROUND extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Round";
  }

  public boolean execute(boolean enabled) {
    if (tags[1].type == TagType.float32) if (enabled) tags[2].setInt(Math.round(tags[1].getFloat()));
    if (tags[1].type == TagType.float64) if (enabled) tags[2].setLong(Math.round(tags[1].getDouble()));
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
