package jfcontrols.tags;

/** Tag
 *
 * @author pquiring
 */

import javaforce.*;

public class Tag {
  protected int cid;
  protected String name;
  protected int type;
  protected String value;  //cached value
  protected boolean dirty;

  /** Create temp tag. */
  public Tag(String name, int type, String value) {
    this.cid = 0;
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public Tag(int cid, String name, int type, SQL sql) {
    this.name = name;
    this.type = type;
    value = "0";
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    dirty = true;
    this.value = value;
  }

  public boolean getBoolean() {
    return !value.equals("0");
  }

  public void setBoolean(boolean value) {
    dirty = true;
    this.value = value ? "1" : "0";
  }

  public int getInt() {
    return Integer.valueOf(value);
  }

  public void setInt(int value) {
    dirty = true;
    this.value = Integer.toString(value);
  }

  public long getLong() {
    return Long.valueOf(value);
  }

  public void setLong(long value) {
    dirty = true;
    this.value = Long.toString(value);
  }

  public float getFloat() {
    return Float.valueOf(value);
  }

  public void setFloat(float value) {
    dirty = true;
    this.value = Float.toString(value);
  }

  public double getDouble() {
    return Double.valueOf(value);
  }

  public void setDouble(double value) {
    dirty = true;
    this.value = Double.toString(value);
  }
}
