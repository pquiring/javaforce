package jfcontrols.logic;

/**  Alarm not ack
 *
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class ALARM_NOT_ACK extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmNotAck";
  }

  public boolean execute(boolean enabled) {
    return enabled & FunctionRuntime.alarm_not_ack();
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
