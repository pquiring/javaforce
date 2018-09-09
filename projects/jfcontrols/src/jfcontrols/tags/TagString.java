package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagString extends TagBase {
  public static final long serialVersionUID = 1;
  public TagString(int cid, int tid, String name, int length) {
    type = javaforce.controls.TagType.string;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    this.unsigned = unsigned;
    isArray = true;
    values = new char[length];
  }
  public char[] values;
  public String toString(int idx) {if (idx >= values.length) return ""; return Character.toString((char)(values[idx] & 0xff));}
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

  public String getString8() {
    int len = 0;
    for(int a=0;a<values.length;a++) {
      if (values[a] == 0) break;
      len++;
    }
    return new String(values, 0, len);
  }
}
