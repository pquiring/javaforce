package jfcontrols.logic;

import javaforce.controls.*;

/**  Alarm not ack
 *
 */

public class ALARM_NOT_ACK extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmNotAck";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "enabled &= alarm_not_active();";
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
