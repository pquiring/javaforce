package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagShort extends TagBase {
  public TagShort() {}
  public TagShort(int cid, int tid, String name, boolean unsigned, int length) {
    type = javaforce.controls.TagType.int16;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    if (length == 0) {
      isArray = false;
      values = new short[1];
    } else {
      isArray = true;
      values = new short[length];
    }
  }
  public short[] values;
  public String toString(int idx) {return unsigned ? Integer.toUnsignedString(values[idx] & 0xffff) : Short.toString(values[idx]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    setDirty();
    values[idx] = (short)(value ? 1 : 0);
  }

  public int getInt(int idx) {
    return values[idx];
  }

  public void setInt(int idx, int value) {
    setDirty();
    values[idx] = (short)value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    setDirty();
    values[idx] = (short)value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    setDirty();
    values[idx] = (short)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    setDirty();
    values[idx] = (short)value;
  }
  public void readObject() throws Exception {
    super.readObject();
    int cnt = readInt();
    values = new short[cnt];
    for(int a=0;a<cnt;a++) {
      values[a] = readShort();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    int cnt = values.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      writeShort(values[a]);
    }
  }
}
