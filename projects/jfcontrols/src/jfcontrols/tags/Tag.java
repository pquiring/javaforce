package jfcontrols.tags;

/** Tag
 *
 * @author pquiring
 */

import javaforce.*;

public class Tag {
  protected int cid;
  protected String name;
  protected int type;
  protected String value;  //cached value
  protected boolean dirty;

  /** Create temp tag. */
  public Tag(String name, int type, String value) {
    this.cid = 0;
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public Tag(int cid, String name, int type, SQL sql) {
    this.name = name;
    this.type = type;
    value = "0";
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    dirty = true;
    this.value = value;
  }

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
      case TagType.BIT:
      case TagType.INT8:
      case TagType.CHAR8:
        return 1;
      case TagType.INT16:
      case TagType.CHAR16:
        return 2;
      case TagType.INT32:
      case TagType.FLOAT32:
        return 4;
      case TagType.INT64:
      case TagType.FLOAT64:
        return 8;
    }
  }

  public int getType() {
    return type;
  }

  public static String decode(int type, byte in[], int pos) {
    boolean unsigned = (type & TagType.UNSIGNED) != 0;
//    boolean array = (type & TagType.UNSIGNED) != 0;
    switch (type & 0xff) {
      case TagType.BIT:
        return in[pos] == 0 ? "0" : "1";
      case TagType.INT8:
        if (unsigned) {
          return Integer.toString(Byte.toUnsignedInt(in[pos]));
        } else {
          return Byte.toString(in[pos]);
        }
      case TagType.INT16:
        if (unsigned) {
          return Integer.toUnsignedString(LE.getuint16(in, pos));
        } else {
          return Integer.toString(LE.getuint16(in, pos));
        }
      case TagType.INT32:
        if (unsigned) {
          return Integer.toUnsignedString(LE.getuint32(in, pos));
        } else {
          return Integer.toString(LE.getuint32(in, pos));
        }
      case TagType.FLOAT32:
        return Float.toString(Float.intBitsToFloat(LE.getuint32(in, pos)));
      case TagType.FLOAT64:
        return Double.toString(Double.longBitsToDouble(LE.getuint64(in, pos)));
    }
    return null;
  }

  public static void encode(int type, String in, byte out[], int pos) {
    boolean unsigned = (type & TagType.UNSIGNED) != 0;
//    boolean array = (type & TagType.UNSIGNED) != 0;
    switch (type & 0xff) {
      case TagType.BIT:
        out[pos] = (byte)(in.equals("0") ? 0 : 1);
        break;
      case TagType.INT8:
        out[pos] = Byte.valueOf(in);
        break;
      case TagType.INT16:
        LE.setuint16(out, pos, Short.valueOf(in));
        break;
      case TagType.INT32:
        LE.setuint32(out, pos, Integer.valueOf(in));
        break;
      case TagType.FLOAT32:
        LE.setuint32(out, pos, Float.floatToIntBits(Float.valueOf(in)));
        break;
      case TagType.FLOAT64:
        LE.setuint64(out, pos, Double.doubleToLongBits(Double.valueOf(in)));
        break;
    }
  }
}
