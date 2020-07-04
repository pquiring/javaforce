package jfcontrols.logic;

/** Get Date
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class GET_DATE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "GetDate";
  }

  public boolean execute(boolean enabled) {
    if (enabled) FunctionRuntime.getdate(tags[1]);
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return IDs.uid_date;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "date";
      default: return null;
    }
  }
}
