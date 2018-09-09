package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagLong extends TagBase {
  public static final long serialVersionUID = 1;
  public TagLong(int cid, int tid, String name, boolean unsigned, int length) {
    type = javaforce.controls.TagType.int64;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    if (length == 0) {
      isArray = false;
      values = new long[1];
    } else {
      isArray = true;
      values = new long[length];
    }
  }
  public long[] values;
  public String toString(int idx) {return unsigned ? Long.toUnsignedString(values[idx] & 0xffffffffL) : Long.toString(values[idx]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    values[idx] = (value ? 1 : 0);
  }

  public int getInt(int idx) {
    return (int)values[idx];
  }

  public void setInt(int idx, int value) {
    values[idx] = value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    values[idx] = value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    values[idx] = (long)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    values[idx] = (long)value;
  }
}
