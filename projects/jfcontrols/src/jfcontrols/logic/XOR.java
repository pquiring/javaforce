package jfcontrols.logic;

import javaforce.controls.*;

import jfcontrols.tags.*;

public class XOR extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "XOR";
  }

  public boolean execute(boolean enabled) {
    if (tags[1].type == TagType.int64) {
      if (enabled) tags[3].setLong(tags[1].getLong() ^ tags[2].getLong());
    } else {
      if (enabled) tags[3].setInt(tags[1].getInt() ^ tags[2].getInt());
    }
    return enabled;
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.anyint;
  }
}
