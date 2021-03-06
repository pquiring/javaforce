package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagChar8 extends TagBase {
  public TagChar8() {}
  public TagChar8(int cid, int tid, String name, int length) {
    type = javaforce.controls.TagType.char8;
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
  public byte[] values;
  public String toString(int idx) {return Character.toString((char)(values[idx] & 0xff));}
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

  public String getString8() {
    if (!isArray) return null;
    int len = 0;
    for(int a=0;a<values.length;a++) {
      if (values[a] == 0) break;
      len++;
    }
    return new String(values, 0, len);
  }
  public void readObject() throws Exception {
    super.readObject();
    writeBytes(values);
  }
  public void writeObject() throws Exception {
    super.writeObject();
    values = readBytes();
  }
}
