package jfcontrols.logic;

/**  Array copy
 *
 *   src and dst may overlap.
 *
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class ARRAY_COPY extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ArrayCopy";
  }

  public boolean execute(boolean enabled) {
    if (enabled) FunctionRuntime.arraycopy(tags);
    return enabled;
  }

  public int getTagsCount() {
    return 5;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyarray;
      case 2: return TagType.anyint;
      case 3: return TagType.anyarray;
      case 4: return TagType.anyint;
      case 5: return TagType.anyint;
    }
    return TagType.unknown;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "src";
      case 2: return "sOff";
      case 3: return "dst";
      case 4: return "dOff";
      case 5: return "len";
      default: return null;
    }
  }
}
