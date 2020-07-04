package jfcontrols.logic;

/**  Alarm active
 *
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class ALARM_ACTIVE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmActive";
  }

  public boolean execute(boolean enabled) {
    return enabled & FunctionRuntime.alarm_active();
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.unknown;
  }

  public String getTagName(int idx) {
    return null;
  }
}
