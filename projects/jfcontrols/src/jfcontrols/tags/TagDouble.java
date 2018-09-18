package jfcontrols.tags;

/**
 *
 * @author User
 */

public class TagDouble extends TagBase {
  public static final long serialVersionUID = 1;
  public TagDouble(int cid, int tid, String name, int length) {
    type = javaforce.controls.TagType.float64;
    this.cid = cid;
    this.tid = tid;
    this.name = name;
    if (length == 0) {
      isArray = false;
      values = new double[1];
    } else {
      isArray = true;
      values = new double[length];
    }
  }
  public double[] values;
  public String toString(int idx) {return Double.toString(values[0]);}
  public int getLength() {return values.length;}
  public boolean getBoolean(int idx) {
    return values[idx] != 0;
  }

  public void setBoolean(int idx, boolean value) {
    setDirty();
    values[idx] = (double)(value ? 1 : 0);
  }

  public int getInt(int idx) {
    return (int)values[idx];
  }

  public void setInt(int idx, int value) {
    setDirty();
    values[idx] = (double)value;
  }

  public long getLong(int idx) {
    return (long)values[idx];
  }

  public void setLong(int idx, long value) {
    setDirty();
    values[idx] = (double)value;
  }

  public float getFloat(int idx) {
    return (float)values[idx];
  }

  public void setFloat(int idx, float value) {
    setDirty();
    values[idx] = (double)value;
  }

  public double getDouble(int idx) {
    return values[idx];
  }

  public void setDouble(int idx, double value) {
    setDirty();
    values[idx] = value;
  }
}
