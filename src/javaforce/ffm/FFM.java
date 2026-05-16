package javaforce.ffm;

/** FFM support class.
 *
 * For Tomcat edit conf/server.xml and add to the <server> section:
 *   <Listener className="org.apache.catalina.core.JniLifecycleListener" libraryPath="c:/programdata/javaforce/jfnative64.dll" />
 * for Linux:
 *   <Listener className="org.apache.catalina.core.JniLifecycleListener" libraryPath="/usr/lib/jfnative64.so" />
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.io.*;
import javaforce.ui.*;
import javaforce.jni.*;
import javaforce.media.*;

public class FFM {
  private static FFM instance;

  /** Returns singleton instance of this class. */
  public static FFM getInstance() {
    if (!enabled) return null;
    if (instance == null) {
      instance = new FFM();
    }
    return instance;
  }

  /** Enabled FFM native inter-ops (default = true)
   */
  private static boolean enabled = true;

  /** Use JNI to pin arrays. */
  private static boolean jni_pinning = true;

  /** Load JF native library to load JNI methods. */
  private static boolean jni_load = false;

  /** Returns FFM enabled state.  */
  public static boolean enabled() {
    if (!enabled) {
      return false;
    }
    getInstance();
    return enabled;
  }

  /** Enable FFM.
   *
   * SymbolLookup will be auto detected to use executable or shared library.
   */
  public static void enable() {
    enabled = true;
  }

  /** Enable FFM.
   *
   * SymbolLookup will be from the supplied library.
   */
  public static void enable(String lib) {
    if (!new File(lib).exists()) {
      JFLog.log("FFM:Error:Library not found:" + lib);
      return;
    }
    FFM.lib = lib;
    enabled = true;
  }

  /** Disable FFM and use JNI instead. */
  public static void disable() {
    enabled = false;
    if (instance != null) {
      instance = null;
      System.gc();
    }
  }

  /** Set wether to load the JF native library to find JNI methods.
   * Default = false
   */
  public static void setLoadLibrary(boolean state) {
    jni_load = state;
  }

  private static String getLibrary() {
    if (JF.isWindows()) {
      return System.getenv("ProgramData") + "/JavaForce/jfnative64.dll";
    } else {
      return "/usr/lib/jfnative64.so";
    }
  }

  private static boolean debug = false;
  private static String lib;  //shared JF library

  private static Linker linker;
  private static Arena arena;
  private static SymbolLookup lookup;
  private static ExecSymbolLookup execlookup;

  private FFM() {
    setupLinker();
    setupUpcalls();
  }

  private static void setupLinker() {
    try {
      linker = Linker.nativeLinker();
      if (lib == null) {
        //auto detect executable or shared library
        if (!JF.isJavaForceLoader()) {
          lib = getLibrary();
          if (!new File(lib).exists()) {
            throw new Exception("FFM:Error:Library not found:" + lib);
          }
        }
      }
      if (lib != null) {
        if (debug) JFLog.log("Loading FFM from:" + lib);
        //symbol lookup from supplied shared library
        arena = Arena.ofAuto();  //freed by gc (which will also close the library at that time)
        lookup = SymbolLookup.libraryLookup(lib, arena);
        if (jni_load) {
          try {System.load(lib);} catch (Throwable t) {JFLog.log(t);}  //load JNI methods
        }
      } else {
        if (debug) JFLog.log("Loading FFM from executable");
        //symbol lookup from executable of this JVM (JavaForce native loader)
        lookup = linker.defaultLookup();
        execlookup = new ExecSymbolLookup();
        execlookup.init();
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }

  /** Assign a new symbol lookup provider. */
  public static void setSymbolLookup(SymbolLookup lookup) {
    if (debug) JFLog.log("lookup=" + lookup);
    FFM.lookup = lookup;
  }

  /** Get native FunctionDescriptor for function with no args and specified return value. */
  public static FunctionDescriptor getFunctionDesciptor(MemoryLayout ret) {
    return FunctionDescriptor.of(ret);
  }

  /** Get native FunctionDescriptor for function with specified return value and arguments. */
  public static FunctionDescriptor getFunctionDesciptor(MemoryLayout ret, MemoryLayout... args) {
    return FunctionDescriptor.of(ret, args);
  }

  /** Get native FunctionDescriptor for function with void return value and arguments. */
  public static FunctionDescriptor getFunctionDesciptorVoid(MemoryLayout... args) {
    return FunctionDescriptor.ofVoid(args);
  }

  /** Get native MethodHandle for specified function name with FunctionDescriptor. */
  public static MethodHandle getFunction(String name, FunctionDescriptor fd) {
    if (debug) JFLog.log("FFM:getFunction:" + name);
    try {
      MemorySegment addr = lookup.findOrThrow(name);
      if (addr == null || addr.address() == 0) {
        JFLog.log("FFM:Function not found(1):" + name + "=" + addr);
        return null;
      }
      return linker.downcallHandle(addr, fd);
    } catch (Exception e) {
      JFLog.logTrace("FFM:Function not found(e):" + name);
      return null;
    }
  }

  /** Get native MethodHandle for specified function name with FunctionDescriptor where the symbol is a function pointer. */
  public static MethodHandle getFunctionPtr(String name, FunctionDescriptor fd) {
    if (debug) JFLog.log("FFM:getFunctionPtr:" + name);
    try {
      MemorySegment addr = lookup.findOrThrow(name);
      if (addr == null || addr.address() == 0) {
        JFLog.log("FFM:FunctionPtr not found(1):" + name + "=" + addr);
        return null;
      }
      //symbols have zero length, need to reinterpret as a pointer
      addr = addr.reinterpret(ADDRESS_SIZE);
      if (addr == null || addr.address() == 0) {
        JFLog.log("FFM:FunctionPtr not found(2):" + name + "=" + addr);
        return null;
      }
      //now get the contents of the pointer
      addr = addr.get(ADDRESS, 0);
      if (addr == null || addr.address() == 0) {
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

  private static ValueLayout classToValueLayout(Class<?> cls) {
    if (cls == boolean.class) return JAVA_BOOLEAN;
    if (cls == byte.class) return JAVA_BYTE;
    if (cls == short.class) return JAVA_SHORT;
    if (cls == char.class) return JAVA_CHAR;
    if (cls == int.class) return JAVA_INT;
    if (cls == long.class) return JAVA_LONG;
    if (cls == float.class) return JAVA_FLOAT;
    if (cls == double.class) return JAVA_DOUBLE;
    return ADDRESS;
  }

  private static Class[] ClassArrayType = new Class[0];

  private static FunctionDescriptor convert(MethodType mt) {
    // Map MethodType return type to MemoryLayout
    Class returnClass = mt.returnType();

    // Map MethodType parameters to MemoryLayouts
    Class[] paramClasses = mt.parameterList().toArray(ClassArrayType);
    int count = paramClasses.length;
    ValueLayout[] paramLayouts = new ValueLayout[paramClasses.length];
    for(int i=0;i<count;i++) {
      Class cls = paramClasses[i];
      paramLayouts[i] = classToValueLayout(cls);
    }

    if (returnClass == void.class) {
      return FunctionDescriptor.ofVoid(paramLayouts);
    } else {
      return FunctionDescriptor.of(classToValueLayout(returnClass), paramLayouts);
    }
  }

  /** Create up call stub to virtual function.
   *
   * UpCalls are EXPENSIVE and should be limited.
   * The JVM tries to cache them and holds on to them for a while causing performance issues.
   */
  public static MemorySegment getFunctionUpCall(Object obj, String method, Class ret, Class[] args, Arena arena) {
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

  /** Create up call stub to static function.
   *
   * UpCalls are EXPENSIVE and should be limited.
   * The JVM tries to cache them and holds on to them for a while causing performance issues.
   */
  public static MemorySegment getFunctionUpCallStatic(Class cls, String method, Class ret, Class[] args, Arena arena) {
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

  /** Create java String from native String (char*) and then free() the native string. */
  public static String getString(MemorySegment ms) {
    if (ms == null || ms.address() == 0) return null;
    String str = ms.reinterpret(Long.MAX_VALUE).getString(0L);
    return str;
  }

  //array helpers

  /** Create native array from float[] */
  public static MemorySegment toMemory(Arena arena, float[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_FLOAT, m);
  }

  /** Create native array from double[] */
  public static MemorySegment toMemory(Arena arena, double[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_DOUBLE, m);
  }

  /** Create native array from long[] */
  public static MemorySegment toMemory(Arena arena, long[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_LONG, m);
  }

  /** Create native array from int[] */
  public static MemorySegment toMemory(Arena arena, int[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_INT, m);
  }

  /** Create native array from short[] */
  public static MemorySegment toMemory(Arena arena, short[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_SHORT, m);
  }

  /** Create native array from byte[] */
  public static MemorySegment toMemory(Arena arena, byte[] m) {
    if (m == null) return MemorySegment.NULL;
    if (jni_pinning) return MemorySegment.ofAddress(JFNative.pin(m));
    return arena.allocateFrom(JAVA_BYTE, m);
  }

  /** Create native array from String[] */
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

  /** Create native array from native void* array */
  public static MemorySegment toMemory(Arena arena, MemorySegment[] ptrs) {
    if (ptrs == null) return MemorySegment.NULL;
    MemorySegment array = arena.allocate(ADDRESS, ptrs.length);
    int idx = 0;
    for(MemorySegment ptr : ptrs) {
      array.setAtIndex(ADDRESS, idx++, ptr);
    }
    return array;
  }

  //array helpers (critical)

  /** Create native array from float[] */
  public static MemorySegment toMemory(float[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  /** Create native array from double[] */
  public static MemorySegment toMemory(double[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  /** Create native array from long[] */
  public static MemorySegment toMemory(long[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  /** Create native array from int[] */
  public static MemorySegment toMemory(int[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  /** Create native array from short[] */
  public static MemorySegment toMemory(short[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  /** Create native array from byte[] */
  public static MemorySegment toMemory(byte[] m) {
    if (m == null) return MemorySegment.NULL;
    return MemorySegment.ofArray(m);
  }

  private static final long JAVA_LONG_SIZE = JAVA_LONG.byteSize();
  private static final long JAVA_INT_SIZE = JAVA_INT.byteSize();
  private static final long JAVA_SHORT_SIZE = JAVA_SHORT.byteSize();
  private static final long JAVA_BYTE_SIZE = JAVA_BYTE.byteSize();
  private static final long ADDRESS_SIZE = ADDRESS.byteSize();

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, float[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      float[] r = seg.toArray(JAVA_FLOAT);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, double[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      double[] r = seg.toArray(JAVA_DOUBLE);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, long[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      long[] r = seg.toArray(JAVA_LONG);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, int[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      int[] r = seg.toArray(JAVA_INT);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, short[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      short[] r = seg.toArray(JAVA_SHORT);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, byte[] m) {
    if (seg == null || m == null) return;
    if (jni_pinning) {
      JFNative.unpin(m, seg.address(), true);
    } else {
      byte[] r = seg.toArray(JAVA_BYTE);
      System.arraycopy(r, 0, m, 0, m.length);
    }
  }

  /** Copy back native array after function returns. */
  public static void copyBack(MemorySegment seg, String[] m) {
    //nop
  }

  private static ThreadLocal<FFMArray> FFMArrayBin = new ThreadLocal<>();

  public static void setFFMArray(FFMArray array) {
    FFMArrayBin.set(array);
  }

  private static long FFMArray_Pin() {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return 0;
    return instance.pin();
  }

  private static void FFMArray_Unpin() {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return;
    instance.unpin();
  }

  private static long FFMArray_NewByteArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewByteArray(size);
  }

  private static long FFMArray_NewShortArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewShortArray(size);
  }

  private static long FFMArray_NewIntArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewIntArray(size);
  }

  private static long FFMArray_NewLongArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewLongArray(size);
  }

  private static long FFMArray_NewFloatArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewFloatArray(size);
  }

  private static long FFMArray_NewDoubleArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewDoubleArray(size);
  }

  private static long FFMArray_NewStringArray(int size) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return -1;
    return instance.NewStringArray(size);
  }

  private static void FFMArray_SetStringElement(int idx, MemorySegment str) {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return;
    instance.SetStringElement(idx, str);
  }

  public static Object getArray() {
    FFMArray instance = FFMArrayBin.get();
    if (instance == null) return null;
    return instance.getArray();
  }

  private static ThreadLocal<MediaIO> MediaIOBin = new ThreadLocal<>();

  public static void setMediaIO(MediaIO io) {
    MediaIOBin.set(io);
  }

  private static int MediaIO_read(MemorySegment data, int size) {
    MediaIO instance = MediaIOBin.get();
    if (instance == null) return -1;
    byte[] byteArray = new byte[size];
    int read = instance.read(byteArray);
    if (read > 0) {
      MemorySegment.copy(byteArray, 0, data.reinterpret(read), JAVA_BYTE, 0, read);
    }
    return read;
  }

  private static int MediaIO_write(MemorySegment data, int size) {
    MediaIO instance = MediaIOBin.get();
    if (instance == null) return -1;
    byte[] byteArray = data.reinterpret(size).asSlice(0, size).toArray(JAVA_BYTE);
    return instance.write(byteArray);
  }

  private static long MediaIO_seek(long pos, int how) {
    MediaIO instance = MediaIOBin.get();
    if (instance == null) return -1;
    return instance.seek(pos, how);
  }

  private static ThreadLocal<FolderListener> FolderListenerBin = new ThreadLocal<>();

  public static void setFolderListener(FolderListener listener) {
    FolderListenerBin.set(listener);
  }

  private static void FolderListener_folderChangeEvent(MemorySegment event, MemorySegment path) {
    FolderListener instance = FolderListenerBin.get();
    if (instance == null) return;
    String event_str = event.reinterpret(1024).getString(0);
    if (debug) JFLog.log("event=" + event_str);
    String path_str = path.reinterpret(1024).getString(0);
    if (debug) JFLog.log("path=" + path_str);
    instance.folderChangeEvent(event_str, path_str);
  }

  private static ThreadLocal<UIEvents> UIEventsBin = new ThreadLocal<>();

  public static void setUIEvents(UIEvents events) {
    UIEventsBin.set(events);
  }

  private static void UIEvents_dispatchEvent(int type, int v1, int v2) {
    UIEvents instance = UIEventsBin.get();
    if (instance == null) return;
    instance.dispatchEvent(type, v1, v2);
  }

  private static Arena global;

  public static MemorySegment upcall_FFMArray;
  public static MemorySegment upcall_FFMArray_Pin;
  public static MemorySegment upcall_FFMArray_Unpin;
  public static MemorySegment upcall_FFMArray_NewByteArray;
  public static MemorySegment upcall_FFMArray_NewShortArray;
  public static MemorySegment upcall_FFMArray_NewIntArray;
  public static MemorySegment upcall_FFMArray_NewLongArray;
  public static MemorySegment upcall_FFMArray_NewFloatArray;
  public static MemorySegment upcall_FFMArray_NewDoubleArray;
  public static MemorySegment upcall_FFMArray_NewStringArray;
  public static MemorySegment upcall_FFMArray_SetStringElement;


  public static MemorySegment upcall_MediaIO;
  public static MemorySegment upcall_MediaIO_read;
  public static MemorySegment upcall_MediaIO_write;
  public static MemorySegment upcall_MediaIO_seek;

  public static MemorySegment upcall_FolderListener_folderChangeEvent;

  public static MemorySegment upcall_UIEvents_dispatchEvent;

  /** Setup static C up calls.
   *
   * Upcalls are used from C to invoke Java methods.
   */
  public static void setupUpcalls() {
    if (debug) JFLog.log("FFM.setupUpcalls");
    if (global != null) return;
    global = Arena.global();
    if (linker == null) {
      setupLinker();
    }
    Class cls = FFM.class;
    upcall_FFMArray_Pin = getFunctionUpCallStatic(cls, "FFMArray_Pin", long.class, new Class[] {}, global);
    upcall_FFMArray_Unpin = getFunctionUpCallStatic(cls, "FFMArray_Unpin", void.class, new Class[] {}, global);
    upcall_FFMArray_NewByteArray = getFunctionUpCallStatic(cls, "FFMArray_NewByteArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewShortArray = getFunctionUpCallStatic(cls, "FFMArray_NewShortArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewIntArray = getFunctionUpCallStatic(cls, "FFMArray_NewIntArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewLongArray = getFunctionUpCallStatic(cls, "FFMArray_NewLongArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewFloatArray = getFunctionUpCallStatic(cls, "FFMArray_NewFloatArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewDoubleArray = getFunctionUpCallStatic(cls, "FFMArray_NewDoubleArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_NewStringArray = getFunctionUpCallStatic(cls, "FFMArray_NewStringArray", long.class, new Class[] {int.class}, global);
    upcall_FFMArray_SetStringElement = getFunctionUpCallStatic(cls, "FFMArray_SetStringElement", void.class, new Class[] {int.class, MemorySegment.class}, global);

    upcall_FFMArray = toMemory(global, new MemorySegment[] {
      upcall_FFMArray_Pin,
      upcall_FFMArray_Unpin,
      upcall_FFMArray_NewByteArray,
      upcall_FFMArray_NewShortArray,
      upcall_FFMArray_NewIntArray,
      upcall_FFMArray_NewLongArray,
      upcall_FFMArray_NewFloatArray,
      upcall_FFMArray_NewDoubleArray,
      upcall_FFMArray_NewStringArray,
      upcall_FFMArray_SetStringElement,
    });

    upcall_MediaIO_read = getFunctionUpCallStatic(cls, "MediaIO_read", int.class, new Class[] {MemorySegment.class, int.class}, global);
    upcall_MediaIO_write = getFunctionUpCallStatic(cls, "MediaIO_write", int.class, new Class[] {MemorySegment.class, int.class}, global);
    upcall_MediaIO_seek = getFunctionUpCallStatic(cls, "MediaIO_seek", long.class, new Class[] {long.class, int.class}, global);

    upcall_MediaIO = toMemory(global, new MemorySegment[] {
      upcall_MediaIO_read,
      upcall_MediaIO_write,
      upcall_MediaIO_seek,
    });

    upcall_FolderListener_folderChangeEvent = getFunctionUpCallStatic(cls, "FolderListener_folderChangeEvent", void.class, new Class[] {MemorySegment.class, MemorySegment.class}, global);

    upcall_UIEvents_dispatchEvent = getFunctionUpCallStatic(cls, "UIEvents_dispatchEvent", void.class, new Class[] {int.class, int.class, int.class}, global);

    //pass FFMArray upcalls to native system
    MethodHandle setup = FFM.getFunctionPtr("_set_upcall_FFMArray", FFM.getFunctionDesciptorVoid(ADDRESS));

    try {
      setup.invokeExact(upcall_FFMArray);
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
