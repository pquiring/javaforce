package javaforce.ffm;

import java.io.*;
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

  public static boolean debug = true;

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
  public MemorySegment struct;

  private void init() {
    if (debug) JFLog.log("FFMStruct.init() " + getClass().getName());
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
      int field_size = getFieldSize(jfield);
      int align_size = getFieldAlignment(jfield);
      int padding = getPadding(offset, align_size);
      offset += padding;
      fields[i] = new FFMField(offset, name, flags);
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
    String md = "/" + clsName.replace(".", "/") + "-fields.md";
    try {
      InputStream is = getClass().getResourceAsStream(md);
      if (is == null) {
        JFLog.log("Error:meta data not found:" + md);
        return in;
      }
      String[] lns = new String(is.readAllBytes()).split("\n");
      int sidx = 0;
      for(String ln : lns) {
        for(int iidx = 0;iidx < cnt; iidx++) {
          Field field = in[iidx];
          if (field == null) continue;
          if (field.getName().equals(ln)) {
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

  private int getPadding(int offset, int size) {
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
      int size = 0;
      for(FFMStruct substruct : substructs) {
        size += substruct.getSize();
      }
      return size;
    }
    if (JF.isDerivedFrom(type, FFMType.Integer.class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Long.class)) {
      return 8;
    }
    if (JF.isDerivedFrom(type, FFMType.Integer[].class)) {
      FFMType.Integer[] types = (FFMType.Integer[])getValue(jfield);
      return types.length * 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Long[].class)) {
      FFMType.Long[] types = (FFMType.Long[])getValue(jfield);
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
    if (JF.isDerivedFrom(type, FFMType.Integer.class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Long.class)) {
      return 8;
    }
    if (JF.isDerivedFrom(type, FFMType.Integer[].class)) {
      return 4;
    }
    if (JF.isDerivedFrom(type, FFMType.Long[].class)) {
      return 8;
    }
    JFLog.log("FFMStruct.getFieldAlignment:Unknown field type:" + type);
    return 0;
  }

  private int getLargestFieldAlignment() {
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

  public MemorySegment marshall(Arena arena) {
    if (debug) JFLog.log("FFMStruct.marshall() " + getClass().getName());
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
          struct_copy(value, sfield.offset, arena);
        } else {
          FFMStruct substruct = (FFMStruct)value;
          sfield.ptr = substruct.marshall(arena).address();
          struct.set(JAVA_LONG, sfield.offset, sfield.ptr);
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMStruct[].class)) {
        FFMStruct[] substructs = (FFMStruct[])value;
        int cnt = substructs.length;
        if (sfield.inline) {
          int offset = sfield.offset;
          for(int i=0;i<cnt;i++) {
            FFMStruct substruct = substructs[i];
            struct_copy(substruct, offset, arena);
            offset += substruct.getSize();
          }
        } else {
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
      if (JF.isDerivedFrom(type, FFMType.Integer.class)) {
        FFMType.Integer ffmtype = (FFMType.Integer)value;
        struct.set(JAVA_INT, sfield.offset, ffmtype.value);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Long.class)) {
        FFMType.Long ffmtype = (FFMType.Long)value;
        struct.set(JAVA_LONG, sfield.offset, ffmtype.value);
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Integer[].class)) {
        FFMType.Integer[] ffmtypes = (FFMType.Integer[])value;
        if (sfield.inline) {
          //TODO
        } else {
          //TODO
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMType.Long[].class)) {
        FFMType.Long[] ffmtype = (FFMType.Long[])value;
        if (sfield.inline) {
          //TODO
        } else {
          //TODO
        }
        continue;
      }
      JFLog.log("FFMStruct.marshall:Unknown field type:" + sfield);
    }
    if (debug) JFLog.log("FFMStruct.marshall:struct=" + struct);
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
      if (JF.isDerivedFrom(type, FFMStruct.class)) {
        if (sfield.inline) {
          //nop
        } else {
          FFMStruct substruct = (FFMStruct)getValue(jfield);
          substruct.unmarshall();
        }
        continue;
      }
      if (JF.isDerivedFrom(type, FFMStruct[].class)) {
        if (sfield.inline) {
          //nop
        } else {
          FFMStruct[] substructs = (FFMStruct[])getValue(jfield);
          for(FFMStruct substruct : substructs) {
            substruct.unmarshall();
          }
        }
        continue;
      }
      JFLog.log("FFMStruct.unmarshall:Unknown field type:" + sfield);
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
    int length = substruct.getSize();
    MemorySegment src = substruct.marshall(arena);
    MemorySegment dst = struct.asSlice(offset, length);
    dst.copyFrom(src);
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

  //generate field order meta data
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: FFMStruct source_folder_in class_folder_out");
      return;
    }
    String src = args[0];
    String dst = args[1];
    int cnt = 0;
    try {
      File[] files = new File(src).listFiles();
      for(File srcfile : files) {
        if (!srcfile.getName().endsWith(".java")) continue;
        String srcfilename = src + "/" + srcfile.getName();
        String dstfilename = dst + "/" + srcfile.getName().replace(".java", "-fields.md");
        File dstFile = new File(dstfilename);
        long srcts = srcfile.lastModified();
        long dstts = dstFile.exists() ? dstFile.lastModified() : 0;
        if (dstts > srcts) continue;
        FileInputStream fis = new FileInputStream(srcfile);
        String[] lns = new String(fis.readAllBytes()).split("\n");
        fis.close();
        boolean struct = false;
        StringBuilder fields = new StringBuilder();
        for(String ln : lns) {
          //find "public class ... extends FFMStruct ..."
          if (!struct) {
            if (ln.indexOf(" class ") != -1 && ln.indexOf(" FFMStruct") != -1) {
              struct = true;
            }
            continue;
          }
          //find "public T field [=value];
          String[] fs = ln.trim().replace(";", "").split("[ ]+");
          if (fs.length < 3) continue;
          if (!fs[0].equals("public")) continue;
          String type = fs[1];
          String name = fs[2];
          fields.append(name);
          fields.append("\n");
        }
        if (struct) {
          //output meta data
          FileOutputStream fos = new FileOutputStream(dstfilename);
          fos.write(fields.toString().getBytes());
          fos.close();
          cnt++;
        }
      }
      if (cnt > 0) System.out.println("Generated meta data for " + cnt + " classes.");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
