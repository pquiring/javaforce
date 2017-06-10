package jfcontrols.logic;

/** Timer On Delay
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.IDs;

public class TIMER_ON_DELAY extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "TimerOnDelay";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "enabled = timer_on_delay(enabled, tags);";
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return IDs.uid_timer;
      case 2: return TagType.int64;
    }
    return TagType.unknown;
  }
}
