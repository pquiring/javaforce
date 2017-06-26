package jfcontrols.logic;

import javaforce.controls.*;

/**  Alarm ack all
 *
 */

public class ALARM_ACK_ALL extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmAckAll";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) alarm_ack_all();";
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
