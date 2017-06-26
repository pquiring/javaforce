package jfcontrols.logic;

import javaforce.controls.*;

/**  Alarm active
 *
 */

public class ALARM_ACTIVE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "AlarmActive";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "enabled &= alarm_active();";
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
