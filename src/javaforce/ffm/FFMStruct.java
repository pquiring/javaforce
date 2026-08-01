package javaforce.ffm;

import java.lang.foreign.*;
import java.lang.reflect.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;

/** FFMStruct.
 *
 * Provides functions to marshall and unmarshall a C-type struct.
 *
 * @author pquiring
 */

public class FFMStruct {

  public static final int FLAG_INLINE = 0x0001;

  public static class FFMField {
    public int offset;
    public String name;
    public Object value;
    public long ptr;
    public boolean inline;
    public FFMField(int offset, String name, int flags) {
      this.offset = offset;
      this.name = name;
      inline = (flags & FLAG_INLINE) == FLAG_INLINE;
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

  private int size = -1;
  private MemorySegment struct;

  private void init() {
    Field[] jfields = getClass().getDeclaredFields();
    int cnt = jfields.length;
    fields = new FFMField[cnt];
    int offset = 0;
    for(int i=0;i<cnt;i++) {
      String name = jfields[i].getName();
      int flags = 0;
      if (!name.startsWith("ptr_")) flags |= FLAG_INLINE;
      fields[i] = new FFMField(offset, name, flags);
      int field_size = getFieldSize(jfields[i]);
      offset += field_size;
    }
    size = offset;
  }

  private int getFieldSize(Field jfield) {
    String name = jfield.getName();
    if (name.startsWith("ptr_")) return 8;
    Class<?> type = jfield.getType();
    if (type == byte.class) return 1;
    if (type == short.class) return 2;
    if (type == int.class) return 4;
    if (type == long.class) return 8;
    if (type == float.class) return 4;
    if (type == double.class) return 8;
    if (type == byte[].class) {
      byte[] array = (byte[])getValue(jfield);
      return array.length;
    }
    if (type == short[].class) {
      short[] array = (short[])getValue(jfield);
      return array.length * 2;
    }
    if (type == int[].class) {
      int[] array = (int[])getValue(jfield);
      return array.length * 4;
    }
    if (type == long[].class) {
      long[] array = (long[])getValue(jfield);
      return array.length * 8;
    }
    if (type == float[].class) {
      float[] array = (float[])getValue(jfield);
      return array.length * 4;
    }
    if (type == double[].class) {
      double[] array = (double[])getValue(jfield);
      return array.length * 8;
    }
    if (type.isAssignableFrom(FFMStruct.class)) {
      FFMStruct substruct = (FFMStruct)getValue(jfield);
      return substruct.getSize();
    }
    if (type.isAssignableFrom(FFMStruct[].class)) {
      FFMStruct[] substructs = (FFMStruct[])getValue(jfield);
      int size = 0;
      for(FFMStruct substruct : substructs) {
        size += substruct.getSize();
      }
      return size;
    }
    JFLog.log("FFMStruct:Unknown field type:" + type);
    return 0;
  }

  public int getSize() {
    if (size == -1) {
      init();
    }
    return size;
  }

  public MemorySegment marshall(Arena arena) {
    if (size == -1) {
      init();
    }
    if (fields == null) return MemorySegment.NULL;
    struct = alloc(arena);
    if (struct == null) return MemorySegment.NULL;
    for(FFMField sfield : fields) {
      if (sfield.name  == null) continue;
      Field jfield = getField(sfield.name);
      if (jfield == null) continue;
      Class<?> type = jfield.getType();
      Object value = getValue(jfield);
      if (value == null) continue;
      if (type == byte.class) {
        struct.set(JAVA_BYTE, sfield.offset, (byte)value);
        continue;
      }
      if (type == short.class) {
        struct.set(JAVA_SHORT, sfield.offset, (short)value);
        continue;
      }
      if (type == int.class) {
        struct.set(JAVA_INT, sfield.offset, (int)value);
        continue;
      }
      if (type == long.class) {
        struct.set(JAVA_LONG, sfield.offset, (long)value);
        continue;
      }
      if (type == float.class) {
        struct.set(JAVA_FLOAT, sfield.offset, (float)value);
        continue;
      }
      if (type == double.class) {
        struct.set(JAVA_DOUBLE, sfield.offset, (double)value);
        continue;
      }
      if (type == String.class) {
        String str = (String)value;
        sfield.ptr = arena.allocateFrom(str).address();
        struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        continue;
      }
      if (type == String[].class) {
        String[] strs = (String[])value;
        sfield.ptr = FFM.toMemory(arena, strs).address();
        struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        continue;
      }
      if (isPrimitiveArray(type)) {
        if (sfield.inline) {
          array_copy(value, sfield.offset, type);
        } else {
          sfield.value = value;
          sfield.pin();
          struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        }
        continue;
      }
      if (type.isAssignableFrom(FFMStruct.class)) {
        if (sfield.inline) {
          struct_copy(value, sfield.offset, arena);
        } else {
          FFMStruct substruct = (FFMStruct)value;
          sfield.ptr = substruct.marshall(arena).address();
          struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        }
        continue;
      }
      if (type.isAssignableFrom(FFMStruct[].class)) {
        if (sfield.inline) {
          FFMStruct[] substructs = (FFMStruct[])value;
          int cnt = substructs.length;
          int offset = sfield.offset;
          for(int i=0;i<cnt;i++) {
            FFMStruct substruct = substructs[i];
            struct_copy(substruct, offset, arena);
            offset += substruct.getSize();
          }
        } else {
          FFMStruct[] substructs = (FFMStruct[])value;
          int cnt = substructs.length;
          MemorySegment array = arena.allocate(JAVA_LONG, cnt);
          for(int i=0;i<cnt;i++) {
            FFMStruct substruct = substructs[i];
            long ptr = substruct.marshall(arena).address();
            array.set(JAVA_LONG, i, ptr);
          }
          return array;
        }
        continue;
      }
      JFLog.log("FFMStruct:Unknown field type:" + sfield);
    }
    return struct;
  }

  public void unmarshall() {
    if (fields == null) return;
    for(FFMField sfield : fields) {
      if (sfield.name  == null) continue;
      Field jfield = getField(sfield.name);
      if (jfield == null) continue;
      Class<?> type = jfield.getType();
      if (type == byte.class) {
        setValue(jfield, struct.get(JAVA_BYTE, sfield.offset));
        continue;
      }
      if (type == short.class) {
        setValue(jfield, struct.get(JAVA_SHORT, sfield.offset));
        continue;
      }
      if (type == int.class) {
        setValue(jfield, struct.get(JAVA_INT, sfield.offset));
        continue;
      }
      if (type == long.class) {
        setValue(jfield, struct.get(JAVA_LONG, sfield.offset));
        continue;
      }
      if (type == float.class) {
        setValue(jfield, struct.get(JAVA_FLOAT, sfield.offset));
        continue;
      }
      if (type == double.class) {
        setValue(jfield, struct.get(JAVA_DOUBLE, sfield.offset));
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
        if (sfield.inline) {
          //nop
        } else {
          sfield.unpin();
        }
        continue;
      }
      if (type.isAssignableFrom(FFMStruct.class)) {
        if (sfield.inline) {
          //TODO
        } else {
          FFMStruct substruct = (FFMStruct)getValue(jfield);
          substruct.unmarshall();
        }
        continue;
      }
      JFLog.log("FFMStruct:Unknown field type:" + sfield);
    }
  }

  private void array_copy(Object array, int offset, Class<?> type) {
    //TODO : major optz
    if (type == byte[].class) {
      byte[] src = (byte[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_BYTE, offset++, src[i]);
      }
      return;
    }
    if (type == short[].class) {
      short[] src = (short[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_SHORT, offset, src[i]);
        offset += 2;
      }
      return;
    }
    if (type == int[].class) {
      int[] src = (int[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_INT, offset, src[i]);
        offset += 4;
      }
      return;
    }
    if (type == long[].class) {
      long[] src = (long[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_LONG, offset, src[i]);
        offset += 8;
      }
      return;
    }
    if (type == float[].class) {
      float[] src = (float[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_FLOAT, offset, src[i]);
        offset += 4;
      }
      return;
    }
    if (type == double[].class) {
      double[] src = (double[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        struct.set(JAVA_DOUBLE, offset, src[i]);
        offset += 8;
      }
      return;
    }
  }

  private void struct_copy(Object value, int offset, Arena arena) {
    FFMStruct substruct = (FFMStruct)value;
    substruct.marshall(arena);
    //TODO : copy substruct to struct @ offset
    substruct.unmarshall();
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

  private MemorySegment alloc(Arena arena) {
    if (size == 0) return null;
    return arena.allocate(JAVA_BYTE, size);
  }
}
