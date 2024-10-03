package javaforce.cl;

/** OpenCL
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public class CL implements AutoCloseable {
  private native static boolean ninit(String opencl);
  public static boolean init() {
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

  private CL(long ctx) {
    this.ctx = ctx;
  }

  public static final int TYPE_DEFAULT = (1 << 0);
  public static final int TYPE_CPU = (1 << 1);
  public static final int TYPE_GPU = (1 << 2);
  public static final int TYPE_ACCELERATOR = (1 << 3);

  private long ctx;

  private static native long ncreate(String src, int type);

  public static CL create(String src, int type) {
    return new CL(ncreate(src, type));
  };

  public static CL create(String src) {
    return create(src, TYPE_DEFAULT);
  }

  private static native long nkernel(long ctx, String func);

  public long kernel(String func) {
    return nkernel(ctx, func);
  }

  private native long ncreateBuffer(long ctx, int size, int type);

  private static final int MEM_READ_WRITE = (1 << 0);
  private static final int MEM_WRITE = (1 << 1);
  private static final int MEM_READ = (1 << 2);

  public long createReadBuffer(int size) {return ncreateBuffer(ctx, size, MEM_READ);}

  public long createWriteBuffer(int size) {return ncreateBuffer(ctx, size, MEM_WRITE);}

  public long createReadWriteBuffer(int size) {return ncreateBuffer(ctx, size, MEM_READ_WRITE);}

  private static native boolean nsetArg(long ctx, long kernel, int idx, byte[] value);

  public boolean setArg(long kernel, int idx, byte[] value) {
    return nsetArg(ctx, kernel, idx, value);
  }

  public boolean setArg(long kernel, int idx, long value) {
    byte[] tmp = new byte[8];
    LE.setuint64(tmp, 0, value);
    return nsetArg(ctx, kernel, idx, tmp);
  }

  private static native boolean nwriteBufferi8(long ctx, long buffer, byte[] value);

  public boolean writeBuffer(long buffer, byte[] data) {
    return nwriteBufferi8(ctx, buffer, data);
  }

  private static native boolean nwriteBufferf32(long ctx, long buffer, float[] value);

  public boolean writeBuffer(long buffer, float[] data) {
    return nwriteBufferf32(ctx, buffer, data);
  }

  private static native boolean nexecute(long ctx, long kernel, int count);

  public boolean execute(long kernel, int count) {return nexecute(ctx, kernel, count);}

  private static native boolean nreadBufferi8(long ctx, long buffer, byte[] value);

  public boolean readBuffer(long buffer, byte[] data) {
    return nreadBufferi8(ctx, buffer, data);
  }

  private static native boolean nreadBufferf32(long ctx, long buffer, float[] value);

  public boolean readBuffer(long buffer, float[] data) {
    return nreadBufferf32(ctx, buffer, data);
  }

  private static native boolean nfreeKernel(long ctx, long kernel);

  public boolean freeKernel(long kernel) {
    return nfreeKernel(ctx, kernel);
  }

  private static native boolean nfreeBuffer(long ctx, long buffer);

  public boolean freeBuffer(long buffer) {
    return nfreeBuffer(ctx, buffer);
  }

  private static native boolean nclose(long ctx);

  public void close() {
    if (ctx != 0) {
      nclose(ctx);
      ctx = 0;
    }
  }

  private static final int SIZE = 64 * 1024;

  public static void main(String[] args) {
    if (!init()) {
      JFLog.log("OpenCL init failed");
      System.exit(1);
    }
    System.out.println("Starting Java test...");
    try {
      CL cl = CL.create(
        "__kernel void square(__global float* input, __global float* output) { int i = get_global_id(0); output[i] = input[i] * input[i]; }",
        TYPE_GPU);
      long kernel = cl.kernel("square");
      long input = cl.createWriteBuffer(Float.BYTES * SIZE);
      long output = cl.createReadBuffer(Float.BYTES * SIZE);

      float[] data = new float[SIZE];
      Random r = new Random();
      for(int i=0;i<SIZE;i++) {
        data[i] = r.nextFloat();
      }
      float[] results = new float[SIZE];

      cl.writeBuffer(input, data);

      cl.setArg(kernel, 0, input);
      cl.setArg(kernel, 1, output);

      cl.execute(kernel, SIZE);

      cl.readBuffer(output, results);

      cl.freeBuffer(input);
      cl.freeBuffer(output);
      cl.freeKernel(kernel);
      cl.close();

      //confirm results
      int correct = 0;
      for(int i=0;i<SIZE;i++) {
        if (results[i] == data[i] * data[i]) {
          correct++;
        }
      }

      System.out.println("java test:" + correct + "/" + SIZE + " are correct");
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
