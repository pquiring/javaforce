package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagChar16 extends TagBase {
  public TagChar16() {}
  public TagChar16(int cid, int tid, String name, int length) {
    type = javaforce.controls.TagType.char16;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    if (length == 0) {
      isArray = false;
      values = new char[1];
    } else {
      isArray = true;
      values = new char[length];
    }
  }
  public char[] values;
  public String toString(int idx) {return Character.toString(values[0]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    values[idx] = (char)(value ? 1 : 0);
  }

  public int getInt(int idx) {
    return values[idx];
  }

  public void setInt(int idx, int value) {
    values[idx] = (char)value;
  }

  public long getLong(int idx) {
    return values[idx];
  }

  public void setLong(int idx, long value) {
    values[idx] = (char)value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    values[idx] = (char)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    values[idx] = (char)value;
  }

  public String getString16() {
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
    int cnt = readInt();
    values = new char[cnt];
    for(int a=0;a<cnt;a++) {
      values[a] = readChar();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    int cnt = values.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      writeChar(values[a]);
    }
  }
}
