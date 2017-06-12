package jfcontrols.logic;

import javaforce.controls.*;

public class SHL extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "SHL";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    if (types[1] == TagType.int64) return "if (enabled) tags[3].setLong(tags[1].getLong() << tags[2].getInt());\r\n";
    return "if (enabled) tags[3].setInt(tags[1].getInt() << tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.anyint;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "val";
      case 2: return "pos";
      case 3: return "res";
      default: return null;
    }
  }
}
