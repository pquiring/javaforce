package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagInt extends TagBase {
  public TagInt(boolean unsigned) {
    type = javaforce.controls.TagType.int32;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
  }
  public TagInt(int value) {
    //create temp tag
    isArray = false;
    values = new int[1];
    values[0] = value;
  }
  public TagInt(int cid, int tid, String name, boolean unsigned, int length) {
    type = javaforce.controls.TagType.int32;
    if (unsigned) type |= javaforce.controls.TagType.unsigned_mask;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    if (length == 0) {
      isArray = false;
      values = new int[1];
    } else {
      isArray = true;
      values = new int[length];
    }
  }
  public int[] values;
  public String toString(int idx) {return unsigned ? Integer.toUnsignedString(values[idx]) : Integer.toString(values[idx]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    values[idx] = (value ? 1 : 0);
  }

  public int getInt(int idx) {
    return values[idx];
  }

  public void setInt(int idx, int value) {
    values[idx] = value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    values[idx] = (int)value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    values[idx] = (int)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    values[idx] = (int)value;
  }

  public void readObject() throws Exception {
    super.readObject();
    int cnt = readInt();
    values = new int[cnt];
    for(int a=0;a<cnt;a++) {
      values[a] = readInt();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    int cnt = values.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      writeInt(values[a]);
    }
  }
}
