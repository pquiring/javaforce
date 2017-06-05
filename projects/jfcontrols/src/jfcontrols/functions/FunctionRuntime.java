package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import javaforce.*;

import jfcontrols.tags.*;

public class FunctionRuntime {
  public IndexTags it = new IndexTags();
  public String getValue(TagAddr addr) {
    TagBase tag = TagsService.getTag(addr, it);
    return tag.getValue(addr);
  }
  public void setValue(TagAddr addr, String value) {
    TagBase tag = TagsService.getTag(addr, it);
    tag.setValue(addr, value);
  }
  public boolean getBoolean(TagAddr addr) {
    return !getValue(addr).equals("0");
  }

  public void setBoolean(TagAddr addr, boolean value) {
    setValue(addr, value ? "1" : "0");
  }

  public int getInt(TagAddr addr) {
    return Integer.valueOf(getValue(addr));
  }

  public void setInt(TagAddr addr, int value) {
    setValue(addr, Integer.toString(value));
  }

  public long getLong(TagAddr addr) {
    return Long.valueOf(getValue(addr));
  }

  public void setLong(TagAddr addr, long value) {
    setValue(addr, Long.toString(value));
  }

  public float getFloat(TagAddr addr) {
    return Float.valueOf(getValue(addr));
  }

  public void setFloat(TagAddr addr, float value) {
    setValue(addr, Float.toString(value));
  }

  public double getDouble(TagAddr addr) {
    return Double.valueOf(getValue(addr));
  }

  public void setDouble(TagAddr addr, double value) {
    setValue(addr, Double.toString(value));
  }
}
