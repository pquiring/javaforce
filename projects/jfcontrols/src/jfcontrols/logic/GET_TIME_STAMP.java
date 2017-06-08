package jfcontrols.logic;

/** Get Time Stamp
 *
 * Returns milliseconds since epoch.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class GET_TIME_STAMP extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "GetTime";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) tags[1].setLong(System.currentTimeMillis());";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.int64;
  }
}
