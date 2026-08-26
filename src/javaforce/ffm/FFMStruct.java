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

  public static boolean debug = false;
  public static boolean debug_struct = false;

  public static final int FLAG_INLINE = 0x0001;
  public static final int FLAG_PTR_PTRARRAY = 0x0002;

  public static class FFMField {
    public int index;
    public int offset;
    public String name;
    public Object value;
    public long ptr;
    public boolean inline;
    public boolean ptr_ptrarray;
    public MemorySegment array;  //for FFMType.Integer[] and FFMType.Long[]

    public FFMField(int offset, String name, int flags) {
      this.offset = offset;
      this.name = name;
      inline = (flags & FLAG_INLINE) == FLAG_INLINE;
      ptr_ptrarray = (flags & FLAG_PTR_PTRARRAY) == FLAG_PTR_PTRARRAY;
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
  public MemorySegment src, dst;

  private void init() {
    Field[] jfields = getClass().getDeclaredFields();
    jfields = sort(jfields);
    int cnt = jfields.length;
    fields = new FFMField[cnt];
    int offset = 0;
    for(int i=0;i<cnt;i++) {
      Field jfield = jfields[i];
      if (jfield == null) {
        JFLog.log("Error:Field == null");
        continue;
      }
      String name = jfield.getName();
      int flags = 0;
      if (!name.startsWith("ptr_")) flags |= FLAG_INLINE;
      if (name.startsWith("ptr_ptrarray_")) flags |= FLAG_PTR_PTRARRAY;
      int field_size = getFieldSize(jfield);
      int align_size = getFieldAlignment(jfield);
      int padding = getPadding(offset, align_size);
      offset += padding;
      fields[i] = new FFMField(offset, name, flags);
      fields[i].index = i;
      offset += field_size;
    }
    if (offset == 0) {
      JFLog.log("FFMStruct.init:Error:size == 0");
    }
    size = offset;
  }

  private Field[] sort(Field[] in) {
    int cnt = in.length;
    Field[] sorted = new Field[cnt];
    //load field order meta data
    String clsName = getClass().getName();
    Meta meta = Meta.getMetaData(clsName);
    try {
      int sidx = 0;
      for(String field_name : meta.fields) {
        for(int iidx = 0;iidx < cnt; iidx++) {
          Field field = in[iidx];
          if (field == null) continue;
          if (field.getName().equals(field_name)) {
            sorted[sidx++] = field;
            in[iidx] = null;
            break;
          }
        }
      }
      return sorted;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private static int getPadding(int offset, int size) {
    if (size == 1) return 0;
    int mod = (offset % size);
    if (mod == 0) return 0;
    return size - mod;
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
    if (JF.isDerivedFrom(type, FFMStruct.class)) {
      FFMStruct substruct = (FFMStruct)getValue(jfield);
      return substruct.getSize();
    }
    if (JF.isDerivedFrom(type, FFMStruct[].class)) {
      FFMStruct[] substructs = (FFMStruct[])getValue(jfield);
      if (substructs.length == 0) return 0;
      int size = 0;
      int align = substructs[0].getLargestFieldAlignment();
      for(FFMStruct substruct : substructs) {
        size += getPadding(size, align);
        size += substruct.getSize();
      }
      return size;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint32.class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint64.class)) {
      return 8;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint32[].class)) {
      FFMType.Uint32[] types = (FFMType.Uint32[])getValue(jfield);
      return types.length * 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint64[].class)) {
      FFMType.Uint64[] types = (FFMType.Uint64[])getValue(jfield);
      return types.length * 8;
    }
    JFLog.log("FFMStruct.getFieldSize:Unknown field type:" + type + ",name=" + jfield.getName());
    return 0;
  }

  private int getFieldAlignment(Field jfield) {
    String name = jfield.getName();
    if (name.startsWith("ptr_")) return 8;
    Class<?> type = jfield.getType();
    if (type == byte.class) return 1;
    if (type == short.class) return 2;
    if (type == int.class) return 4;
    if (type == long.class) return 8;
    if (type == float.class) return 4;
    if (type == double.class) return 8;
    if (type == byte[].class) return 1;
    if (type == short[].class) return 2;
    if (type == int[].class) return 4;
    if (type == long[].class) return 8;
    if (type == float[].class) return 4;
    if (type == double[].class) return 8;
    if (JF.isDerivedFrom(type, FFMStruct.class)) {
      FFMStruct substruct = (FFMStruct)getValue(jfield);
      return substruct.getLargestFieldAlignment();
    }
    if (JF.isDerivedFrom(type, FFMStruct[].class)) {
      FFMStruct[] substructs = (FFMStruct[])getValue(jfield);
      int max_align = 0;
      for(FFMStruct substruct : substructs) {
        int this_align = substruct.getLargestFieldAlignment();
        if (this_align > max_align) {
          max_align = this_align;
        }
      }
      return max_align;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint32.class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint64.class)) {
      return 8;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint32[].class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Uint64[].class)) {
      return 8;
    }
    JFLog.log("FFMStruct.getFieldAlignment:Unknown field type:" + type);
    return 0;
  }

  private int getLargestFieldAlignment() {
    if (fields == null) {
      init();
    }
    int max_align = 0;
    for(FFMField field : fields) {
      int this_align = getFieldAlignment(getField(field.name));
      if (this_align > max_align) {
        max_align = this_align;
      }
    }
    return max_align;
  }

  public int getSize() {
    if (size == -1) {
      init();
    }
    return size;
  }

  public MemorySegment getMemorySegment() {
    return struct;
  }

  public MemorySegment marshall(Arena arena) {
    if (size == -1) {
      init();
    }
    if (fields == null) {
      JFLog.log("FFMStruct.marshall:Error:fields == null");
      return MemorySegment.NULL;
    }
    struct = alloc(arena);
    if (struct == null) {
      JFLog.log("FFMStruct.marshall:Error:struct == null");
      return MemorySegment.NULL;
    }
    if (debug) JFLog.log("FFMStruct.marshall() " + getClass().getName() + "@" + struct);
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
      if (JF.isDerivedFrom(type, FFMStruct.class)) {
        if (sfield.inline) {
          struct_copy(value, struct, sfield.offset, arena);
        } else {
          FFMStruct substruct = (FFMStruct)value;
          sfield.ptr = substruct.marshall(arena).address();
          struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMStruct[].class)) {
        FFMStruct[] substructs = (FFMStruct[])value;
        if (substructs.length == 0) continue;
        int align = substructs[0].getLargestFieldAlignment();
        int cnt = substructs.length;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(int i=0;i<cnt;i++) {
            FFMStruct substruct = substructs[i];
            offset += getPadding(offset, align);
            struct_copy(substruct, struct, offset, arena);
            offset += substruct.getSize();
          }
        } else {
          if (sfield.ptr_ptrarray) {
            MemorySegment array = arena.allocate(JAVA_LONG, cnt);
            for(int i=0;i<cnt;i++) {
              FFMStruct substruct = substructs[i];
              long ptr = substruct.marshall(arena).address();
              array.setAtIndex(JAVA_LONG, i, ptr);
            }
            struct.set(JAVA_LONG, sfield.offset, array.address());
          } else {
            int size = substructs[0].getSize();
            MemorySegment array = arena.allocate(JAVA_BYTE, cnt * size);
            int offset = 0;
            for(int i=0;i<cnt;i++) {
              FFMStruct substruct = substructs[i];
              offset += getPadding(offset, align);
              struct_copy(substruct, array, offset, arena);
              offset += substruct.getSize();
            }
            struct.set(JAVA_LONG, sfield.offset, array.address());
          }
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint32.class)) {
        FFMType.Uint32 ffmtype = (FFMType.Uint32)value;
        struct.set(JAVA_INT, sfield.offset, ffmtype.value);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint64.class)) {
        FFMType.Uint64 ffmtype = (FFMType.Uint64)value;
        struct.set(JAVA_LONG, sfield.offset, ffmtype.value);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint32[].class)) {
        FFMType.Uint32[] ffmtypes = (FFMType.Uint32[])value;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(FFMType.Uint32 ffmtype : ffmtypes) {
            struct.set(JAVA_INT, offset, ffmtype.value);
            offset += 4;
          }
        } else {
          int cnt = ffmtypes.length;
          MemorySegment array = arena.allocate(JAVA_BYTE, cnt * 4);
          for(int i=0;i<cnt;i++) {
            FFMType.Uint32 ffmtype = ffmtypes[i];
            array.setAtIndex(JAVA_INT, i, ffmtype.getValue());
          }
          struct.set(JAVA_LONG, sfield.offset, array.address());
          sfield.array = array;
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint64[].class)) {
        FFMType.Uint64[] ffmtypes = (FFMType.Uint64[])value;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(FFMType.Uint64 ffmtype : ffmtypes) {
            struct.set(JAVA_LONG, offset, ffmtype.value);
            offset += 8;
          }
        } else {
          int cnt = ffmtypes.length;
          MemorySegment array = arena.allocate(JAVA_BYTE, cnt * 8);
          for(int i=0;i<cnt;i++) {
            FFMType.Uint64 ffmtype = ffmtypes[i];
            array.setAtIndex(JAVA_LONG, i, ffmtype.getValue());
          }
          struct.set(JAVA_LONG, sfield.offset, array.address());
          sfield.array = array;
        }
        continue;
      }
      JFLog.log("FFMStruct.marshall:Unknown field type:" + type);
    }
    if (debug_struct) {
      print();
    }
    return struct;
  }

  public void unmarshall() {
    if (debug) JFLog.log("FFMStruct.unmarshall() " + getClass().getName() + "@" + struct);
    if (fields == null) {
      JFLog.log("FFMStruct.unmarshall() Error:fields == null");
      return;
    }
    if (struct == null) {
      JFLog.log("FFMStruct.unmarshall() Error:struct == null");
      return;
    }
    for(FFMField sfield : fields) {
      if (sfield.name  == null) continue;
      Field jfield = getField(sfield.name);
      if (jfield == null) continue;
      Class<?> type = jfield.getType();
      Object value = getValue(jfield);
      if (value == null) continue;
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
          array_copy_back(value, sfield.offset, type);
        } else {
          sfield.unpin();
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMStruct.class)) {
        if (sfield.inline) {
          struct_copy_back(value);
        } else {
          FFMStruct substruct = (FFMStruct)getValue(jfield);
          substruct.unmarshall();
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMStruct[].class)) {
        FFMStruct[] substructs = (FFMStruct[])value;
        int cnt = substructs.length;
        if (sfield.inline) {
          for(int i=0;i<cnt;i++) {
            FFMStruct substruct = substructs[i];
            struct_copy_back(substruct);
          }
        } else {
          if (sfield.ptr_ptrarray) {
            for(FFMStruct substruct : substructs) {
              substruct.unmarshall();
            }
          } else {
            for(FFMStruct substruct : substructs) {
              struct_copy_back(substruct);
            }
          }
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint32.class)) {
        FFMType.Uint32 ffmtype = (FFMType.Uint32)value;
        ffmtype.value = struct.get(JAVA_INT, sfield.offset);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint64.class)) {
        FFMType.Uint64 ffmtype = (FFMType.Uint64)value;
        ffmtype.value = struct.get(JAVA_LONG, sfield.offset);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint32[].class)) {
        FFMType.Uint32[] ffmtypes = (FFMType.Uint32[])value;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(FFMType.Uint32 ffmtype : ffmtypes) {
            ffmtype.setValue(struct.get(JAVA_INT, offset));
            offset += 4;
          }
        } else {
          int cnt = ffmtypes.length;
          MemorySegment array = sfield.array;
          for(int i=0;i<cnt;i++) {
            FFMType.Uint32 ffmtype = ffmtypes[i];
            ffmtype.setValue(array.getAtIndex(JAVA_INT, i));
          }
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Uint64[].class)) {
        FFMType.Uint64[] ffmtypes = (FFMType.Uint64[])value;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(FFMType.Uint64 ffmtype : ffmtypes) {
            ffmtype.setValue(struct.get(JAVA_LONG, offset));
            offset += 8;
          }
        } else {
          int cnt = ffmtypes.length;
          MemorySegment array = sfield.array;
          for(int i=0;i<cnt;i++) {
            FFMType.Uint64 ffmtype = ffmtypes[i];
            ffmtype.setValue(array.getAtIndex(JAVA_LONG, i));
          }
        }
        continue;
      }
      JFLog.log("FFMStruct.unmarshall:Unknown field type:" + type);
    }
    struct = null;
  }

  private void array_copy(Object array, int offset, Class<?> type) {
    //TODO : major optz
    if (type == byte[].class) {
      byte[] src = (byte[])array;
      int len = src.length;
      if (debug) JFLog.log("array_copy:byte[] " + array + ":offset=" + offset + ":len=" + len);
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

  private void array_copy_back(Object array, int offset, Class<?> type) {
    //TODO : major optz
    if (type == byte[].class) {
      byte[] dst = (byte[])array;
      int len = dst.length;
      if (debug) JFLog.log("array_copy_back:byte[]" + array + ":offset=" + offset + ":len=" + len);
      for(int i=0;i<len;i++) {
        dst[i] = struct.get(JAVA_BYTE, offset++);
      }
      return;
    }
    if (type == short[].class) {
      short[] dst = (short[])array;
      int len = dst.length;
      for(int i=0;i<len;i++) {
        dst[i] = struct.get(JAVA_SHORT, offset);
        offset += 2;
      }
      return;
    }
    if (type == int[].class) {
      int[] dst = (int[])array;
      int len = dst.length;
      for(int i=0;i<len;i++) {
        dst[i] = struct.get(JAVA_INT, offset);
        offset += 4;
      }
      return;
    }
    if (type == long[].class) {
      long[] dst = (long[])array;
      int len = dst.length;
      for(int i=0;i<len;i++) {
        dst[i] = struct.get(JAVA_LONG, offset);
        offset += 8;
      }
      return;
    }
    if (type == float[].class) {
      float[] dst = (float[])array;
      int len = dst.length;
      for(int i=0;i<len;i++) {
        dst[i] = struct.get(JAVA_FLOAT, offset);
        offset += 4;
      }
      return;
    }
    if (type == double[].class) {
      double[] src = (double[])array;
      int len = src.length;
      for(int i=0;i<len;i++) {
        src[i] = struct.get(JAVA_DOUBLE, offset);
        offset += 8;
      }
      return;
    }
  }

  private void struct_copy(Object value, MemorySegment dest, int offset, Arena arena) {
    FFMStruct substruct = (FFMStruct)value;
    int length = substruct.getSize();
    substruct.src = substruct.marshall(arena);
    substruct.dst = dest.asSlice(offset, length);
    substruct.dst.copyFrom(substruct.src);
//    if (debug) JFLog.log("src=" + Long.toHexString(substruct.src.address()) + ":dst=" + Long.toHexString(substruct.dst.address()));
  }

  private void struct_copy_back(Object value) {
    FFMStruct substruct = (FFMStruct)value;
//    if (debug) JFLog.log("src=" + Long.toHexString(substruct.src.address()) + ":dst=" + Long.toHexString(substruct.dst.address()));
    substruct.src.copyFrom(substruct.dst);
    substruct.unmarshall();
    substruct.src = null;
    substruct.dst = null;
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

  public static boolean isStruct(Class<?> type) {
    try {
      return JF.isDerivedFrom(type, FFMStruct.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public static boolean isStruct(Field field) {
    try {
      Class<?> cls = field.getType();
      return JF.isDerivedFrom(cls, FFMStruct.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public void print() {
    if (struct == null) return;
    StringBuilder buf = new StringBuilder();
    buf.append("FFMStruct:" + getClass().getName() + ":\r\n");
    int size = (int)struct.byteSize();
    for(int idx=0;idx<size;idx++) {
      byte b = struct.get(JAVA_BYTE, idx);
      if (idx > 0 && idx % 16 == 0) {
        buf.append("\r\n");
      } else {
        if (idx > 0) buf.append(",");
      }
      buf.append(String.format("%02x", b & 0xff));
    }
    buf.append("\r\n");
    JFLog.log(buf.toString());
  }
}
