package javaforce.ffm;

/** FFM support class.
 *
 * @author pquiring
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;

public class FFM {
  private static FFM instance;

  public static FFM getInstance() {
    if (!enabled) return null;
    if (instance == null) {
      instance = new FFM();
    }
    return instance;
  }

  /** Enabled FFM usage. */
  private static boolean enabled = false;

  public static boolean enabled() {
    if (!enabled) return false;
    getInstance();
    return enabled;
  }

  /** Disable FFM and use JNI instead. */
  public static void disable() {
    enabled = false;
  }

  private static boolean debug = false;

  private Linker linker;
  private SymbolLookup lookup;
  private ExecSymbolLookup execlookup;
  private static MethodHandle jfArrayFree;
  public static void jfArrayFree(MemorySegment arr) { try { jfArrayFree.invokeExact(arr); } catch (Throwable t) { JFLog.log(t); } }

  private FFM() {
    try {
      linker = Linker.nativeLinker();
      lookup = linker.defaultLookup();
      execlookup = new ExecSymbolLookup();
      execlookup.init(this);
      jfArrayFree = getFunctionPtr("_jfArrayFree", getFunctionDesciptorVoid(ADDRESS));
      if (jfArrayFree == null) throw new Exception("FFM:Unable to find jfArrayFree function");
    } catch (Throwable t) {
      JFLog.log(t);
      enabled = false;
    }
  }

  public void setSymbolLookup(SymbolLookup lookup) {
    if (debug) JFLog.log("lookup=" + lookup);
    this.lookup = lookup;
  }

  public FunctionDescriptor getFunctionDesciptor(MemoryLayout ret) {
    return FunctionDescriptor.of(ret);
  }

  public FunctionDescriptor getFunctionDesciptor(MemoryLayout ret, MemoryLayout... args) {
    return FunctionDescriptor.of(ret, args);
  }

  public FunctionDescriptor getFunctionDesciptorVoid(MemoryLayout... args) {
    return FunctionDescriptor.ofVoid(args);
  }

  public MethodHandle getFunction(String name, FunctionDescriptor fd) {
    try {
      MemorySegment addr = lookup.findOrThrow(name);
      if (addr == null) {
        JFLog.log("FFM:Function not found(1):" + name + "=" + addr);
        return null;
      }
      return linker.downcallHandle(addr, fd);
    } catch (Exception e) {
      JFLog.logTrace("FFM:Function not found(e):" + name);
      return null;
    }
  }

  public MethodHandle getFunctionPtr(String name, FunctionDescriptor fd) {
    try {
      MemorySegment addr = lookup.findOrThrow(name);
      if (addr == null) {
        JFLog.log("FFM:FunctionPtr not found(1):" + name + "=" + addr);
        return null;
      }
      //symbols have zero length, need to reinterpret as a pointer
      addr = addr.reinterpret(ADDRESS_SIZE);
      if (addr == null) {
        JFLog.log("FFM:FunctionPtr not found(2):" + name + "=" + addr);
        return null;
      }
      //now get the contents of the pointer
      addr = addr.get(ADDRESS, 0);
      if (addr == null) {
        JFLog.log("FFM:FunctionPtr not found(3):" + name + "=" + addr);
        return null;
      }
      return linker.downcallHandle(addr, fd);
    } catch (Exception e) {
      JFLog.logTrace("FFM:FunctionPtr not found(e):" + name);
      return null;
    }
  }

  //upcall helpers

  public static ValueLayout classToValueLayout(Class<?> cls) {
    if (cls == boolean.class) return ValueLayout.JAVA_BOOLEAN;
    if (cls == byte.class) return ValueLayout.JAVA_BYTE;
    if (cls == short.class) return ValueLayout.JAVA_SHORT;
    if (cls == char.class) return ValueLayout.JAVA_CHAR;
    if (cls == int.class) return ValueLayout.JAVA_INT;
    if (cls == long.class) return ValueLayout.JAVA_LONG;
    if (cls == float.class) return ValueLayout.JAVA_FLOAT;
    if (cls == double.class) return ValueLayout.JAVA_DOUBLE;

    if (cls == byte[].class) return ValueLayout.ADDRESS;

    throw new IllegalArgumentException("Unsupported class for ValueLayout: " + cls.getName());
  }

  private static Class[] ClassArrayType = new Class[0];

  private static FunctionDescriptor convert(MethodType mt) {
    // Map MethodType return type to MemoryLayout
    ValueLayout returnLayout = classToValueLayout(mt.returnType());

    // Map MethodType parameters to MemoryLayouts
    Class[] paramClasses = mt.parameterList().toArray(ClassArrayType);
    int count = paramClasses.length;
    ValueLayout[] paramLayouts = new ValueLayout[paramClasses.length];
    for(int i=0;i<count;i++) {
      Class cls = paramClasses[i];
      paramLayouts[i] = classToValueLayout(cls);
    }

    return FunctionDescriptor.of(returnLayout, paramLayouts);
  }

  /** Create up call stub to virtual function. */
  public MemorySegment getFunctionUpCall(Object obj, String method, Class ret, Class[] args, Arena arena) {
    MethodType mt;
    MethodHandle mh, bmh;
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    mt = MethodType.methodType(ret, args);
    try {
      mh = lookup.findVirtual(obj.getClass(), method, mt);
      bmh = mh.bindTo(obj);
      return linker.upcallStub(bmh, convert(mt), arena);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Create up call stub to static function. */
  public MemorySegment getFunctionUpCallStatic(Class cls, String method, Class ret, Class[] args, Arena arena) {
    MethodType mt;
    MethodHandle mh;
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    mt = MethodType.methodType(ret, args);
    try {
      mh = lookup.findStatic(cls, method, mt);
      return linker.upcallStub(mh, convert(mt), arena);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  //array helpers

  public static MemorySegment toMemory(Arena arena, float[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_FLOAT, m);
  }

  public static MemorySegment toMemory(Arena arena, double[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_DOUBLE, m);
  }

  public static MemorySegment toMemory(Arena arena, long[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_LONG, m);
  }

  public static MemorySegment toMemory(Arena arena, int[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_INT, m);
  }

  public static MemorySegment toMemory(Arena arena, short[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_SHORT, m);
  }

  public static MemorySegment toMemory(Arena arena, byte[] m) {
    if (m == null) return MemorySegment.NULL;
    return arena.allocateFrom(JAVA_BYTE, m);
  }

  public static MemorySegment toMemory(Arena arena, String[] strs) {
    if (strs == null) return MemorySegment.NULL;
    MemorySegment ptrs = arena.allocate(ADDRESS, strs.length);
    int idx = 0;
    for(String str : strs) {
      MemorySegment ba = arena.allocateFrom(str);
      ptrs.setAtIndex(ADDRESS, idx++, ba);
    }
    return ptrs;
  }

  public static MemorySegment toMemory(Arena arena, MemorySegment[] ptrs) {
    if (ptrs == null) return MemorySegment.NULL;
    MemorySegment array = arena.allocate(ADDRESS, ptrs.length);
    int idx = 0;
    for(MemorySegment ptr : ptrs) {
      array.setAtIndex(ADDRESS, idx++, ptr);
    }
    return array;
  }

  private static final long JAVA_LONG_SIZE = JAVA_LONG.byteSize();
  private static final long JAVA_INT_SIZE = JAVA_INT.byteSize();
  private static final long JAVA_SHORT_SIZE = JAVA_SHORT.byteSize();
  private static final long JAVA_BYTE_SIZE = JAVA_BYTE.byteSize();
  private static final long ADDRESS_SIZE = ADDRESS.byteSize();
  private static final long JFARRAY_HEADER_SIZE = JAVA_INT_SIZE * 2;

  /*
   Functions that return arrays use this struct:
   template<typename T, int N>
   struct JFArray {
     int count;
     short size;  //not used in Java
     short type;  //not used in Java
     T elements[N];
   };
   */

  public static String[] toArrayString(MemorySegment m) {
    m = m.reinterpret(JFARRAY_HEADER_SIZE);
    int count = m.getAtIndex(JAVA_INT, 0);
    long size = JFARRAY_HEADER_SIZE + count * ADDRESS_SIZE;
    MemorySegment arr = m.reinterpret(size).asSlice(JFARRAY_HEADER_SIZE, count * ADDRESS_SIZE);
    String[] ret = new String[count];
    for(int i=0;i<count;i++) {
      ret[i] = arr.getAtIndex(ADDRESS, i).reinterpret(Integer.MAX_VALUE).getString(0);
    }
    jfArrayFree(m);
    return ret;
  }

  public static long[] toArrayLong(MemorySegment m) {
    m = m.reinterpret(JFARRAY_HEADER_SIZE);
    int count = m.getAtIndex(JAVA_INT, 0);
    long size = JFARRAY_HEADER_SIZE + count * JAVA_LONG_SIZE;
    MemorySegment arr = m.reinterpret(size).asSlice(JFARRAY_HEADER_SIZE, count * JAVA_LONG_SIZE);
    long[] ret = new long[count];
    for(int i=0;i<count;i++) {
      ret[i] = arr.getAtIndex(JAVA_LONG, i);
    }
    jfArrayFree(m);
    return ret;
  }

  public static int[] toArrayInt(MemorySegment m) {
    m = m.reinterpret(JFARRAY_HEADER_SIZE);
    int count = m.getAtIndex(JAVA_INT, 0);
    long size = JFARRAY_HEADER_SIZE + count * JAVA_INT_SIZE;
    MemorySegment arr = m.reinterpret(size).asSlice(JFARRAY_HEADER_SIZE, count * JAVA_INT_SIZE);
    int[] ret = new int[count];
    for(int i=0;i<count;i++) {
      ret[i] = arr.getAtIndex(JAVA_INT, i);
    }
    jfArrayFree(m);
    return ret;
  }

  public static short[] toArrayShort(MemorySegment m) {
    m = m.reinterpret(JFARRAY_HEADER_SIZE);
    int count = m.getAtIndex(JAVA_INT, 0);
    long size = JFARRAY_HEADER_SIZE + count * JAVA_SHORT_SIZE;
    MemorySegment arr = m.reinterpret(size).asSlice(JFARRAY_HEADER_SIZE, count * JAVA_SHORT_SIZE);
    short[] ret = new short[count];
    for(int i=0;i<count;i++) {
      ret[i] = arr.getAtIndex(JAVA_SHORT, i);
    }
    jfArrayFree(m);
    return ret;
  }

  public static byte[] toArrayByte(MemorySegment m) {
    m = m.reinterpret(JFARRAY_HEADER_SIZE);
    int count = m.getAtIndex(JAVA_INT, 0);
    long size = JFARRAY_HEADER_SIZE + count * JAVA_BYTE_SIZE;
    MemorySegment arr = m.reinterpret(size).asSlice(JFARRAY_HEADER_SIZE, count * JAVA_BYTE_SIZE);
    byte[] ret = new byte[count];
    for(int i=0;i<count;i++) {
      ret[i] = arr.getAtIndex(JAVA_BYTE, i);
    }
    jfArrayFree(m);
    return ret;
  }

  public static void copyBack(MemorySegment seg, float[] m) {
    if (seg == null || m == null) return;
    float[] r = seg.toArray(JAVA_FLOAT);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, double[] m) {
    if (seg == null || m == null) return;
    double[] r = seg.toArray(JAVA_DOUBLE);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, long[] m) {
    if (seg == null || m == null) return;
    long[] r = seg.toArray(JAVA_LONG);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, int[] m) {
    if (seg == null || m == null) return;
    int[] r = seg.toArray(JAVA_INT);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, short[] m) {
    if (seg == null || m == null) return;
    short[] r = seg.toArray(JAVA_SHORT);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, byte[] m) {
    if (seg == null || m == null) return;
    byte[] r = seg.toArray(JAVA_BYTE);
    System.arraycopy(r, 0, m, 0, m.length);
  }

  public static void copyBack(MemorySegment seg, String[] m) {
    //nop
  }
}
