package jfcontrols.logic;

/** Timer On Delay
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class TIMER_ON_DELAY extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "TimerOnDelay";
  }

  public boolean execute(boolean enabled) {
    return FunctionRuntime.timer_on_delay(enabled, tags);
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
