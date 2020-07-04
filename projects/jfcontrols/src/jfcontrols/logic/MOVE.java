package jfcontrols.logic;

/** Move / Convert
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class MOVE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Move";
  }

  public boolean execute(boolean enabled) {
    if (!enabled) return false;

    switch (tags[1].type) {
      case TagType.float32:
        switch (tags[2].type) {
          case TagType.float32: tags[2].setFloat(tags[1].getFloat()); break;
          case TagType.float64: tags[2].setDouble(tags[1].getFloat()); break;
          case TagType.int64: tags[2].setLong((long)tags[1].getFloat()); break;
          default: tags[2].setInt((int)tags[1].getFloat()); break;
        }
        break;
      case TagType.float64:
        switch (tags[2].type) {
          case TagType.float32: tags[2].setFloat((float)tags[1].getDouble()); break;
          case TagType.float64: tags[2].setDouble(tags[1].getDouble()); break;
          case TagType.int64: tags[2].setLong((long)tags[1].getDouble()); break;
          default: tags[2].setInt((int)tags[1].getDouble()); break;
        }
        break;
      case TagType.int64:
        switch (tags[2].type) {
          case TagType.float32: tags[2].setFloat((float)tags[1].getLong()); break;
          case TagType.float64: tags[2].setDouble((double)tags[1].getLong()); break;
          case TagType.int64: tags[2].setLong((long)tags[1].getLong()); break;
          default: tags[2].setInt((int)tags[1].getLong()); break;
        }
        break;
      default:
        switch (tags[2].type) {
          case TagType.float32: tags[2].setFloat((float)tags[1].getInt()); break;
          case TagType.float64: tags[2].setDouble((double)tags[1].getInt()); break;
          case TagType.int64: tags[2].setLong((long)tags[1].getInt()); break;
          default: tags[2].setInt(tags[1].getInt()); break;
        }
        break;
    }
    return true;
  }

  public int getTagsCount() {
    return 2;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }

  public String getTagName(int idx) {
    switch (idx) {
      case 1: return "src";
      case 2: return "dst";
      default: return null;
    }
  }
}
