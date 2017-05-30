package jfcontrols.tags;

/** Tag
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.controls.*;
import jfcontrols.sql.SQLService;

public abstract class TagBase {
  protected int cid;
  protected String name;
  protected int type;
  protected String value;  //cached value
  protected HashMap<Integer, TagValue> values;
  protected boolean unsigned;
  protected boolean array;
  protected boolean dirty;

  /** Create temp tag. */
  public TagBase(String name, int type, String value) {
    this.cid = 0;
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public TagBase(int cid, String name, int type, boolean array, boolean unsigned, SQL sql) {
    this.name = name;
    this.type = type;
    this.unsigned = unsigned;
    this.array = array;
    value = "0";
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public abstract String getValue(int idx);

  public void setValue(String value) {
    dirty = true;
    this.value = value;
  }

  public abstract void setValue(String value, int idx);

  public boolean getBoolean() {
    return !value.equals("0");
  }

  public void setBoolean(boolean value) {
    dirty = true;
    this.value = value ? "1" : "0";
  }

  public int getInt() {
    return Integer.valueOf(value);
  }

  public void setInt(int value) {
    dirty = true;
    this.value = Integer.toString(value);
  }

  public long getLong() {
    return Long.valueOf(value);
  }

  public void setLong(long value) {
    dirty = true;
    this.value = Long.toString(value);
  }

  public float getFloat() {
    return Float.valueOf(value);
  }

  public void setFloat(float value) {
    dirty = true;
    this.value = Float.toString(value);
  }

  public double getDouble() {
    return Double.valueOf(value);
  }

  public void setDouble(double value) {
    dirty = true;
    this.value = Double.toString(value);
  }

  public int getSize() {
    return getSize(type);
  }

  public static int getSize(int type) {
    switch (type) {
      default:
      case TagType.bit:
      case TagType.int8:
      case TagType.char8:
        return 1;
      case TagType.int16:
      case TagType.char16:
        return 2;
      case TagType.int32:
      case TagType.float32:
        return 4;
      case TagType.int64:
      case TagType.float64:
        return 8;
    }
  }

  public int getType() {
    return type;
  }

  public boolean isArray() {
    return array;
  }

  public boolean isUnsigned() {
    return unsigned;
  }

  public static String decode(int type, boolean unsigned, byte in[], int pos) {
    switch (type & 0xff) {
      case TagType.bit:
        return in[pos] == 0 ? "0" : "1";
      case TagType.int8:
        if (unsigned) {
          return Integer.toString(Byte.toUnsignedInt(in[pos]));
        } else {
          return Byte.toString(in[pos]);
        }
      case TagType.int16:
        if (unsigned) {
          return Integer.toUnsignedString(LE.getuint16(in, pos));
        } else {
          return Integer.toString(LE.getuint16(in, pos));
        }
      case TagType.int32:
        if (unsigned) {
          return Integer.toUnsignedString(LE.getuint32(in, pos));
        } else {
          return Integer.toString(LE.getuint32(in, pos));
        }
      case TagType.float32:
        return Float.toString(Float.intBitsToFloat(LE.getuint32(in, pos)));
      case TagType.float64:
        return Double.toString(Double.longBitsToDouble(LE.getuint64(in, pos)));
    }
    return null;
  }

  public static void encode(int type, boolean unsigned, String in, byte out[], int pos) {
    switch (type & 0xff) {
      case TagType.bit:
        out[pos] = (byte)(in.equals("0") ? 0 : 1);
        break;
      case TagType.int8:
        out[pos] = Byte.valueOf(in);
        break;
      case TagType.int16:
        LE.setuint16(out, pos, Short.valueOf(in));
        break;
      case TagType.int32:
        LE.setuint32(out, pos, Integer.valueOf(in));
        break;
      case TagType.float32:
        LE.setuint32(out, pos, Float.floatToIntBits(Float.valueOf(in)));
        break;
      case TagType.float64:
        LE.setuint64(out, pos, Double.doubleToLongBits(Double.valueOf(in)));
        break;
    }
  }
}
