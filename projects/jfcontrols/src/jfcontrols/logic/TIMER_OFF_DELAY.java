package jfcontrols.logic;

/** Timer Off Delay
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class TIMER_OFF_DELAY extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "TimerOffDelay";
  }

  public boolean execute(boolean enabled) {
    enabled = FunctionRuntime.timer_off_delay(enabled, tags);
    return enabled;
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

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "timer";
      case 2: return "ms";
      default: return null;
    }
  }
}
