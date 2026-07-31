package javaforce.ffm;

import java.lang.foreign.*;
import java.lang.reflect.*;

import javaforce.*;

/** FFMStruct.
 *
 * Provides functions to marshall and unmarshall a C-type struct.
 *
 * @author pquiring
 */

public class FFMStruct {

  public static class FFMField {
    public int offset;
    public String name;
    public Object value;
    public long ptr;
    public FFMField(int offset, String name) {
      this.offset = offset;
      this.name = name;
    }
    public long pin() {
      if (value == null) return 0;
      ptr = JFHeap.pin(value);
      return ptr;
    }
    public void unpin() {
      if (ptr == 0) return;
      JFHeap.unpin(value, ptr, true);
      value = null;
      ptr = 0;
    }
  }

  public FFMField[] fields;

  private int endian;
  private int size;
  private byte[] struct;
  private long ptr;

  public void setupStruct(int endian, int size, FFMField[] fields) {
    this.endian = endian;
    this.size = size;
    this.fields = fields;
  }

  public long marshall(Arena arena) {
    if (fields == null) return 0;
    struct = alloc();
    if (struct == null) return 0;
    for(FFMField sfield : fields) {
      if (sfield.name  == null) continue;
      Field jfield = getField(sfield.name);
      if (jfield == null) continue;
      Class<?> type = jfield.getType();
      Object value = getValue(jfield);
      if (value == null) continue;
      if (type == byte.class) {
        struct[sfield.offset] = (byte)value;
        continue;
      }
      if (type == short.class) {
        if (endian == Endian.L)
          LE.setuint16(struct, sfield.offset, (short)value);
        else
          BE.setuint16(struct, sfield.offset, (short)value);
        continue;
      }
      if (type == int.class) {
        if (endian == Endian.L)
          LE.setuint32(struct, sfield.offset, (int)value);
        else
          BE.setuint32(struct, sfield.offset, (int)value);
        continue;
      }
      if (type == long.class) {
        if (endian == Endian.L)
          LE.setuint64(struct, sfield.offset, (long)value);
        else
          BE.setuint64(struct, sfield.offset, (long)value);
        continue;
      }
      if (type == float.class) {
        if (endian == Endian.L)
          LE.setfloat(struct, sfield.offset, (float)value);
        else
          BE.setfloat(struct, sfield.offset, (float)value);
        continue;
      }
      if (type == double.class) {
        if (endian == Endian.L)
          LE.setdouble(struct, sfield.offset, (double)value);
        else
          BE.setdouble(struct, sfield.offset, (double)value);
        continue;
      }
      if (type == String.class) {
        String str = (String)value;
        sfield.ptr = arena.allocateFrom(str).address();

        if (endian == Endian.L)
          LE.setuint64(struct, sfield.offset, sfield.ptr);
        else
          BE.setuint64(struct, sfield.offset, sfield.ptr);
        continue;
      }
      if (type == String[].class) {
        String[] strs = (String[])value;
        sfield.ptr = FFM.toMemory(arena, strs).address();

        if (endian == Endian.L)
          LE.setuint64(struct, sfield.offset, sfield.ptr);
        else
          BE.setuint64(struct, sfield.offset, sfield.ptr);
        continue;
      }
      if (isPrimitiveArray(type)) {
        sfield.value = value;
        sfield.pin();
        if (endian == Endian.L)
          LE.setuint64(struct, sfield.offset, sfield.ptr);
        else
          BE.setuint64(struct, sfield.offset, sfield.ptr);
        continue;
      }
      if (type.isAssignableFrom(FFMStruct.class)) {
        FFMStruct substruct = (FFMStruct)value;
        if (endian == Endian.L)
          LE.setuint64(struct, sfield.offset, substruct.marshall(arena));
        else
          BE.setuint64(struct, sfield.offset, substruct.marshall(arena));
        continue;
      }
      JFLog.log("FFMStruct:Unknown field type:" + sfield);
    }
    return pin();
  }

  public void unmarshall() {
    if (fields == null) return;
    for(FFMField sfield : fields) {
      if (sfield.name  == null) continue;
      Field jfield = getField(sfield.name);
      if (jfield == null) continue;
      Class<?> type = jfield.getType();
      if (type == byte.class) {
        setValue(jfield, struct[sfield.offset]);
        continue;
      }
      if (type == short.class) {
        if (endian == Endian.L)
          setValue(jfield, LE.getuint16(struct, sfield.offset));
        else
          setValue(jfield, BE.getuint16(struct, sfield.offset));
        continue;
      }
      if (type == int.class) {
        if (endian == Endian.L)
          setValue(jfield, LE.getuint32(struct, sfield.offset));
        else
          setValue(jfield, BE.getuint32(struct, sfield.offset));
        continue;
      }
      if (type == long.class) {
        if (endian == Endian.L)
          setValue(jfield, LE.getuint64(struct, sfield.offset));
        else
          setValue(jfield, BE.getuint64(struct, sfield.offset));
        continue;
      }
      if (type == float.class) {
        if (endian == Endian.L)
          setValue(jfield, LE.getfloat(struct, sfield.offset));
        else
          setValue(jfield, BE.getfloat(struct, sfield.offset));
        continue;
      }
      if (type == double.class) {
        if (endian == Endian.L)
          setValue(jfield, LE.getdouble(struct, sfield.offset));
        else
          setValue(jfield, BE.getdouble(struct, sfield.offset));
        continue;
      }
      if (type == String.class) {
        //no copy back
        continue;
      }
      if (type == String[].class) {
        //no copy back
        continue;
      }
      if (isPrimitiveArray(type)) {
        sfield.unpin();
        continue;
      }
      if (type.isAssignableFrom(FFMStruct.class)) {
        FFMStruct substruct = (FFMStruct)getValue(jfield);
        if (endian == Endian.L)
          substruct.ptr = LE.getuint64(struct, sfield.offset);
        else
          substruct.ptr = BE.getuint64(struct, sfield.offset);
        substruct.unmarshall();
        continue;
      }
      JFLog.log("FFMStruct:Unknown field type:" + sfield);
    }
    unpin();
  }

  private boolean isPrimitiveArray(Class<?> type) {
    if (type == byte[].class) return true;
    if (type == short[].class) return true;
    if (type == int[].class) return true;
    if (type == long[].class) return true;
    if (type == float[].class) return true;
    if (type == double[].class) return true;
    return false;
  }

  private Field getField(String name) {
    try {
      return getClass().getField(name);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private Object getValue(Field field) {
    try {
      return field.get(this);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private void setValue(Field field, Object value) {
    try {
      field.set(this, value);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private byte[] alloc() {
    if (size == 0) return null;
    return new byte[size];
  }

  private long pin() {
    ptr = JFHeap.pin(struct);
    return ptr;
  }

  private void unpin() {
    if (ptr == 0) return;
    JFHeap.unpin(struct, ptr, true);
    struct = null;
    ptr = 0;
  }
}
