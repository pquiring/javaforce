package javaforce.jni;

/** OpenCL JNI implementation
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.cl.*;

public class CLJNI implements CL {

  private static CLJNI instance;

  public synchronized static CLJNI getInstance() {
    if (instance == null) {
      instance = new CLJNI();
      if (!instance.init()) {
        instance = null;
        return null;
      }
    }
    return instance;
  }

  private native boolean ninit(String lib);
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
    return ninit(libs[0].path);
  }

  public native long create(String src, int type);

  public long create(String src) {
    return create(src, TYPE_DEFAULT);
  }

  public native long kernel(long ctx, String func);

  public native long createBuffer(long ctx, int size, int type);

  public long createReadBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_READ);}

  public long createWriteBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_WRITE);}

  public long createReadWriteBuffer(long ctx, int size) {return createBuffer(ctx, size, MEM_READ_WRITE);}

  public native boolean setArg(long ctx, long kernel, int idx, byte[] value);

  public boolean setArg(long ctx, long kernel, int idx, int value) {
    byte[] tmp = new byte[4];
    LE.setuint32(tmp, 0, value);
    return setArg(ctx, kernel, idx, tmp);
  }

  public boolean setArg(long ctx, long kernel, int idx, long value) {
    byte[] tmp = new byte[8];
    LE.setuint64(tmp, 0, value);
    return setArg(ctx, kernel, idx, tmp);
  }

  public native boolean writeBufferi8(long ctx, long buffer, byte[] value);

  public boolean writeBuffer(long ctx, long buffer, byte[] data) {
    return writeBufferi8(ctx, buffer, data);
  }

  public native boolean writeBufferf32(long ctx, long buffer, float[] value);

  public boolean writeBuffer(long ctx, long buffer, float[] data) {
    return writeBufferf32(ctx, buffer, data);
  }

  public native boolean execute(long ctx, long kernel, int count);

  public native boolean execute2(long ctx, long kernel, int count1, int count2);

  public native boolean execute3(long ctx, long kernel, int count1, int count2, int count3);

  public native boolean execute4(long ctx, long kernel, int count1, int count2, int count3, int count4);

  public native boolean readBufferi8(long ctx, long buffer, byte[] value);

  public boolean readBuffer(long ctx, long buffer, byte[] data) {
    return readBufferi8(ctx, buffer, data);
  }

  public native boolean readBufferf32(long ctx, long buffer, float[] value);

  public boolean readBuffer(long ctx, long buffer, float[] data) {
    return readBufferf32(ctx, buffer, data);
  }

  public native boolean freeKernel(long ctx, long kernel);

  public native boolean freeBuffer(long ctx, long buffer);

  public native boolean close(long ctx);

}
