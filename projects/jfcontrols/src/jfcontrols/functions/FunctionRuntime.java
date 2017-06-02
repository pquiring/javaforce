package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import javaforce.*;

import jfcontrols.tags.*;

public class FunctionRuntime {
  public static String getValue(TagAddr addr) {
    TagBase tag = TagsService.getTag(addr);
    return tag.getValue(addr);
  }
  public static void setValue(TagAddr addr, String value) {
    TagBase tag = TagsService.getTag(addr);
    tag.setValue(addr, value);
  }
  public static boolean getBoolean(TagAddr addr) {
    return !getValue(addr).equals("0");
  }

  public static void setBoolean(TagAddr addr, boolean value) {
    setValue(addr, value ? "1" : "0");
  }

  public static int getInt(TagAddr addr) {
    return Integer.valueOf(getValue(addr));
  }

  public static void setInt(TagAddr addr, int value) {
    setValue(addr, Integer.toString(value));
  }

  public static long getLong(TagAddr addr) {
    return Long.valueOf(getValue(addr));
  }

  public static void setLong(TagAddr addr, long value) {
    setValue(addr, Long.toString(value));
  }

  public static float getFloat(TagAddr addr) {
    return Float.valueOf(getValue(addr));
  }

  public static void setFloat(TagAddr addr, float value) {
    setValue(addr, Float.toString(value));
  }

  public static double getDouble(TagAddr addr) {
    return Double.valueOf(getValue(addr));
  }

  public static void setDouble(TagAddr addr, double value) {
    setValue(addr, Double.toString(value));
  }
}
