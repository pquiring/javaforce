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

  public boolean setArg(long kernel, int idx, int value) {
    byte[] tmp = new byte[4];
    LE.setuint32(tmp, 0, value);
    return nsetArg(ctx, kernel, idx, tmp);
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

  private static native boolean nexecute2(long ctx, long kernel, int count1, int count2);

  public boolean execute2(long kernel, int count1, int count2) {return nexecute2(ctx, kernel, count1, count2);}

  private static native boolean nexecute3(long ctx, long kernel, int count1, int count2, int count3);

  public boolean execute3(long kernel, int count1, int count2, int count3) {return nexecute3(ctx, kernel, count1, count2, count3);}

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

  public static void main(String[] args) {
    Compute compute = new Compute();
    if (!init()) {
      JFLog.log("OpenCL init failed");
      System.exit(1);
    }
    if (!compute.init(TYPE_GPU)) {
      JFLog.log("Compute init failed");
      System.exit(1);
    }
    int SIZE;
    int SIZE_SIZE;
    Random rand = new Random();
    System.out.println("Starting tests...");
    //array_square
    try {
      SIZE = 64 * 1024;
      float[] a = new float[SIZE];
      for(int i=0;i<SIZE;i++) {
        a[i] = rand.nextFloat();
      }
      float[] b = new float[SIZE];

      compute.array_square(a, b);

      //confirm results
      int correct = 0;
      for(int i=0;i<SIZE;i++) {
        if (b[i] == a[i] * a[i]) {
          correct++;
        }
      }

      System.out.println("array_square:" + correct + "/" + SIZE + " are correct");
    } catch (Throwable t) {
      JFLog.log(t);
    }
    //matrix_mult
    try {
      boolean identity = true;
      SIZE = 3;  //3*3 = 9
      SIZE_SIZE = SIZE * SIZE;
      float[] a = new float[SIZE_SIZE];
      float[] b = new float[SIZE_SIZE];
      float[] c = new float[SIZE_SIZE];
      int idx = 0;
      for(int row=0;row<SIZE;row++) {
        for(int col=0;col<SIZE;col++) {
          a[idx] = rand.nextFloat();
          if (identity) {
            b[idx] = (row == col ? 1 : 0);  //identity matrix
          } else {
            b[idx] = rand.nextFloat();
          }
          idx++;
        }
      }
      compute.matrix_mult(SIZE, SIZE, SIZE, a, b, c);

      //confirm results
      int correct = 0;
      for(int row=0;row<SIZE;row++) {
        for(int col=0;col<SIZE;col++) {
          int i = col * SIZE + row;
          float res = 0;
          for(int k=0;k<SIZE;k++) {
            res += a[k * SIZE + row] * b[col * SIZE + k];
          }
          if (c[i] == res) {
            correct++;
          } else {
            JFLog.log("c[] = " + c[i] + ":res=" + res);
          }
        }
      }
      if (true) {
        javaforce.Console.printArray(a);
        javaforce.Console.printArray(b);
        javaforce.Console.printArray(c);
      }
      System.out.println("matrix_mult:" + correct + "/" + SIZE_SIZE + " are correct");
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
