package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagFloat extends TagBase {
  public static final long serialVersionUID = 1;
  public TagFloat(int cid, int tid, String name, int length) {
    type = javaforce.controls.TagType.float32;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    if (length == 0) {
      isArray = false;
      values = new float[1];
    } else {
      isArray = true;
      values = new float[length];
    }
  }
  public TagFloat(float value) {
    //create temp tag
    isArray = false;
    values = new float[1];
    values[0] = value;
  }
  public float[] values;
  public String toString(int idx) {return Float.toString(values[idx]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    values[idx] = (float)(value ? 1 : 0);
  }

  public int getInt(int idx) {
    return (int)values[idx];
  }

  public void setInt(int idx, int value) {
    values[idx] = (float)value;
  }

  public long getLong(int idx) {
    return (long)values[idx];
  }

  public void setLong(int idx, long value) {
    values[idx] = (float)value;
  }

  public float getFloat(int idx) {
    return values[idx];
  }

  public void setFloat(int idx, float value) {
    values[idx] = value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    values[idx] = (float)value;
  }
}
