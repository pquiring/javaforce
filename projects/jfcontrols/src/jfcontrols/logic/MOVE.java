package jfcontrols.logic;

/** Move / Convert
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class MOVE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Move";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    StringBuilder sb = new StringBuilder();
    sb.append("if (enabled) {");

    switch (types[1]) {
      case TagType.float32:
        switch (types[2]) {
          case TagType.float32: sb.append("tags[2].setFloat(tags[1].getFloat());"); break;
          case TagType.float64: sb.append("tags[2].setDouble(tags[1].getFloat());"); break;
          case TagType.int64: sb.append("tags[2].setLong((long)tags[1].getFloat());"); break;
          default: sb.append("tags[2].setInt((int)tags[1].getFloat());"); break;
        }
        break;
      case TagType.float64:
        switch (types[2]) {
          case TagType.float32: sb.append("tags[2].setFloat((float)tags[1].getDouble())"); break;
          case TagType.float64: sb.append("tags[2].setDouble(tags[1].getDouble());"); break;
          case TagType.int64: sb.append("tags[2].setLong((long)tags[1].getDouble());"); break;
          default: sb.append("tags[2].setInt((int)tags[1].getDouble());"); break;
        }
        break;
      case TagType.int64:
        switch (types[2]) {
          case TagType.float32: sb.append("tags[2].setFloat((float)tags[1].getLong())"); break;
          case TagType.float64: sb.append("tags[2].setDouble((double)tags[1].getLong());"); break;
          case TagType.int64: sb.append("tags[2].setLong((long)tags[1].getLong());"); break;
          default: sb.append("tags[2].setInt((int)tags[1].getLong());"); break;
        }
        break;
      default:
        switch (types[2]) {
          case TagType.float32: sb.append("tags[2].setFloat((float)tags[1].getInt())"); break;
          case TagType.float64: sb.append("tags[2].setDouble((double)tags[1].getInt());"); break;
          case TagType.int64: sb.append("tags[2].setLong((long)tags[1].getInt());"); break;
          default: sb.append("tags[2].setInt(tags[1].getInt());"); break;
        }
        break;
    }

    sb.append("}\r\n");
    return sb.toString();
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
