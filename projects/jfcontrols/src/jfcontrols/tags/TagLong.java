package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagLong extends TagBase {
  public TagLong(boolean unsigned) {
    type = javaforce.controls.TagType.int64;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
  }
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
    setDirty();
    values[idx] = (value ? 1 : 0);
  }

  public int getInt(int idx) {
    return (int)values[idx];
  }

  public void setInt(int idx, int value) {
    setDirty();
    values[idx] = value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    setDirty();
    values[idx] = value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    setDirty();
    values[idx] = (long)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    setDirty();
    values[idx] = (long)value;
  }
  public void readObject() throws Exception {
    super.readObject();
    int cnt = readInt();
    values = new long[cnt];
    for(int a=0;a<cnt;a++) {
      values[a] = readLong();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    int cnt = values.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      writeLong(values[a]);
    }
  }
}
