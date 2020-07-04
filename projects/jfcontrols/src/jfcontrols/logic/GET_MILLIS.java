package jfcontrols.logic;

/** Get Time Stamp
 *
 * Returns milliseconds since epoch.
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class GET_MILLIS extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "GetMillis";
  }

  public boolean execute(boolean enabled) {
    if (enabled) tags[1].setLong(System.currentTimeMillis());
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.int64;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "ms";
      default: return null;
    }
  }
}
