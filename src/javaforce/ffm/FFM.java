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
    if (instance == null) {
      instance = new FFM();
    }
    return instance;
  }

  /** Enabled FFM usage. */
  public static boolean enabled = true;

  private static boolean debug = false;

  private Linker linker;
  private SymbolLookup lookup;
  private ExecSymbolLookup execlookup;

  private FFM() {
    try {
      linker = Linker.nativeLinker();
      lookup = linker.defaultLookup();
      execlookup = new ExecSymbolLookup();
      execlookup.init(this);
    } catch (Throwable t) {
      JFLog.log(t);
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
      addr = addr.reinterpret(8);
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
