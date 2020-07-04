package jfcontrols.logic;

/**  Alarm ack all
 *
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class ALARM_ACK_ALL extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmAckAll";
  }

  public boolean execute(boolean enabled) {
    if (enabled) FunctionRuntime.alarm_ack_all();
    return enabled;
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
