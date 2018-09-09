package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagByte extends TagBase {
  public static final long serialVersionUID = 1;
  public TagByte(int cid, int tid, String name, boolean unsigned, int length) {
    type = javaforce.controls.TagType.int8;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    if (length == 0) {
      isArray = false;
      values = new byte[1];
    } else {
      isArray = true;
      values = new byte[length];
    }
  }
  private byte[] values;

  public String toString(int idx) {return unsigned ? Integer.toUnsignedString(values[idx] & 0xff) : Byte.toString(values[idx]);}

  public int getLength() {return values.length;}

  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    values[idx] = (byte)(value ? 1 : 0);
  }

  public int getInt(int idx) {
    return values[idx];
  }

  public void setInt(int idx, int value) {
    values[idx] = (byte)value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    values[idx] = (byte)value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    values[idx] = (byte)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    values[idx] = (byte)value;
  }
}
