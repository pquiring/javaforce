package jfcontrols.logic;

/** Compare Not Equal
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class CMP_NE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Compare !=";
  }

  public boolean execute(boolean enabled) {
    switch (tags[1].type) {
      case TagType.float32: enabled &= (tags[1].getFloat() != tags[2].getFloat()); break;
      case TagType.float64: enabled &= (tags[1].getDouble() !=  tags[2].getDouble()); break;
      case TagType.int64: enabled &= (tags[1].getLong() != tags[2].getLong()); break;
      default: enabled &= (tags[1].getInt() != tags[2].getInt()); break;
    }
    return enabled;
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }
}
