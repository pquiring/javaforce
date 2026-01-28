package javaforce.cl;

/** OpenCL
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;
import javaforce.api.*;
import javaforce.ffm.*;
import javaforce.jni.*;

public class CL {

  public static final int TYPE_DEFAULT = (1 << 0);
  public static final int TYPE_CPU = (1 << 1);
  public static final int TYPE_GPU = (1 << 2);
  public static final int TYPE_ACCELERATOR = (1 << 3);

  private CLAPI api;

  private static CL instance;

  public static CL getInstance() {
    if (instance == null) {
      instance = new CL();
      if (FFM.enabled()) {
        instance.api = CLFFM.getInstance();
      } else {
        instance.api = CLJNI.getInstance();
      }
      if (!instance.init()) {
        return null;
      }
    }
    return instance;
  }

  private boolean init() {
    File[] sysFolders;
    String ext = "";
    String apphome = System.getProperty("java.app.home");
    String name = "OpenCL";
    if (apphome == null) apphome = ".";
    if (JF.isWindows()) {
      sysFolders = new File[] {new File(System.getenv("windir") + "\\system32"), new File(apphome), new File(".")};
      ext = ".dll";
      name = name.toLowerCase();
    } else if (JF.isMac()) {
      sysFolders = new File[] {new File("/System/Library/Frameworks/OpenCL.framework/Versions/Current/Libraries"), new File(apphome), new File(".")};
      ext = ".dylib";
    } else {
      sysFolders = new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())};
      ext = ".so";
    }
    Library[] libs = {new Library(name)};
    JFNative.findLibraries(sysFolders, libs, ext);
    if (libs[0].path == null) {
      JFLog.log("Error:Unable to find OpenCL library");
      return false;
    }
    return api.clLoadLibrary(libs[0].path);
  }

  /** Create OpenGL Context with source code.
   * close() context to release it.
   *
   * @param src = OpenCL source code
   * @param type = TYPE_...
   */
  public long create(String src, int type) {
    return api.clCreate(src, type);
  }

  /** Create OpenGL Context (type = TYPE_DEFAULT).
   * close() context to release it.
   *
   * @param src = OpenCL source code
   */
  public long create(String src) {
    return create(src, TYPE_DEFAULT);
  }

  /** Create OpenGL Kernel.
   * freeKernel() to release it.
   *
   * @param ctx = OpenCL Context
   * @param func = function name
   */
  public long kernel(long ctx, String func) {
    return api.clKernel(ctx, func);
  }

  public static final int MEM_READ_WRITE = (1 << 0);
  public static final int MEM_WRITE = (1 << 1);
  public static final int MEM_READ = (1 << 2);

  public long createBuffer(long ctx, int size, int type) {
    return api.clCreateBuffer(ctx, size, type);
  }

  /** Create a read-only buffer. */
  public long createReadBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_READ);}

  /** Create a write-only buffer. */
  public long createWriteBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_WRITE);}

  /** Create a read-write buffer. */
  public long createReadWriteBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_READ_WRITE);}

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, byte[] value) {
    return api.clSetArg(ctx, kernel, idx, value, value.length);
  }

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, int value) {
    byte[] tmp = new byte[4];
    LE.setuint32(tmp, 0, value);
    return setArg(ctx, kernel, idx, tmp);
  }

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, long value) {
    byte[] tmp = new byte[8];
    LE.setuint64(tmp, 0, value);
    return setArg(ctx, kernel, idx, tmp);
  }

  public boolean writeBufferi8(long ctx, long buffer, byte[] value) {
    return api.clWriteBufferi8(ctx, buffer, value, value.length);
  }

  public boolean writeBufferf32(long ctx, long buffer, float[] value) {
    return api.clWriteBufferf32(ctx, buffer, value, value.length);
  }

  /** Write to a buffer. */
  public boolean writeBuffer(long ctx, long buffer, byte[] data) {
    return writeBufferi8(ctx, buffer, data);
  }

  /** Write to a buffer. */
  public boolean writeBuffer(long ctx, long buffer, float[] data) {
    return writeBufferf32(ctx, buffer, data);
  }

  /** Execute a kernel with one counter. */
  public boolean execute(long ctx, long kernel, int count) {
    return api.clExecute(ctx, kernel, count);
  }

  /** Execute a kernel with two counters. */
  public boolean execute2(long ctx, long kernel, int count1, int count2) {
    return api.clExecute2(ctx, kernel, count1, count2);
  }

  /** Execute a kernel with three counters. */
  public boolean execute3(long ctx, long kernel, int count1, int count2, int count3) {
    return api.clExecute3(ctx, kernel, count1, count2, count3);
  }

  /** Execute a kernel with four counters. */
  public boolean execute4(long ctx, long kernel, int count1, int count2, int count3, int count4) {
    return api.clExecute4(ctx, kernel, count1, count2, count3, count4);
  }

  /** Read from a buffer. */
  public boolean readBuffer(long ctx, long buffer, byte[] data) {
    return api.clReadBufferi8(ctx, buffer, data, data.length);
  }

  /** Read from a buffer. */
  public boolean readBuffer(long ctx, long buffer, float[] data) {
    return api.clReadBufferf32(ctx, buffer, data, data.length);
  }

  /** Release a kernel. */
  public boolean freeKernel(long ctx, long kernel) {
    return api.clFreeKernel(ctx, kernel);
  }

  /** Release a buffer. */
  public boolean freeBuffer(long ctx, long buffer) {
    return api.clFreeBuffer(ctx, buffer);
  }

  /** Release an OpenCL context. */
  public boolean close(long ctx) {
    return api.clClose(ctx);
  }
}
