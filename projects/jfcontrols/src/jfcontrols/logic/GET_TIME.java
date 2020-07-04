package jfcontrols.logic;

/** Get Time
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class GET_TIME extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "GetTime";
  }

  public boolean execute(boolean enabled) {
    if (enabled) FunctionRuntime.gettime(tags[1]);
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return IDs.uid_time;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "time";
      default: return null;
    }
  }
}
