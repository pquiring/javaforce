package jfcontrols.logic;

/** Get Time
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class GET_TIME extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "GetTime";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "gettime(tags[1]);";
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
