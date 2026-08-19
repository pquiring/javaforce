package javaforce.ffm;

import java.lang.reflect.*;

import javaforce.*;

/** FFMType.
 *
 *  Typically stored a native handle or enum type.
 *
 *  TODO : These should be 'value' types when Valhalla is ready.
 *
 * @author pquiring
 */

public abstract class FFMType {

  public static class Integer extends FFMType {
    public int value;
    public String getType() {return "int";}
    public int getValue() {return value;}
    public void setValue(int value) {this.value = value;}
    public void set(Integer value) {this.value = value.value;}
    public Integer() {}
    public Integer(int value) {setValue(value);}
    public Integer(Integer value) {setValue(value.value);}
    public String toString() {return "0x" + java.lang.Integer.toHexString(value);}
  }

  public static class Long extends FFMType {
    public long value;
    public String getType() {return "long";}
    public long getValue() {return value;}
    public void setValue(long value) {this.value = value;}
    public void set(Long value) {this.value = value.value;}
    public Long() {}
    public Long(long value) {setValue(value);}
    public Long(Long value) {setValue(value.value);}
    public String toString() {return "0x" + java.lang.Long.toHexString(value);}
  }

  public static boolean isType(Class<?> type) {
    try {
      return JF.isDerivedFrom(type, FFMType.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
  public static boolean isType(Field field) {
    try {
      Class<?> cls = field.getType();
      return isType(cls);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
  public static boolean isTypeInt(Class<?> cls) {
    try {
      return JF.isDerivedFrom(cls, FFMType.Integer.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
  public static boolean isTypeInt(Field field) {
    try {
      Class<?> cls = field.getType();
      return isTypeInt(cls);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
  public static boolean isTypeLong(Class<?> cls) {
    try {
      return JF.isDerivedFrom(cls, FFMType.Long.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
  public static boolean isTypeLong(Field field) {
    try {
      Class<?> cls = field.getType();
      return isTypeLong(cls);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
}
