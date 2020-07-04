package jfcontrols.logic;

/**  Array size
 *
 */

import javaforce.controls.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

public class ARRAY_SIZE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ArraySize";
  }

  public boolean execute(boolean enabled) {
    if (enabled) FunctionRuntime.arraysize(tags);
    return enabled;
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    switch (idx) {
      case 1: return TagType.anyarray;
      case 2: return TagType.anyint;
    }
    return TagType.unknown;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "array";
      case 2: return "len";
      default: return null;
    }
  }
}
