package javaforce.jni;

/** OpenCL JNI implementation
 *
 * @author pquiring
 */

import javaforce.api.*;

public class CLJNI implements CLAPI {

  private static CLJNI instance;

  public synchronized static CLJNI getInstance() {
    if (instance == null) {
      instance = new CLJNI();
    }
    return instance;
  }

  public native boolean clLoadLibrary(String file);

  public native long clCreate(String src, int type);

  public native long clKernel(long ctx, String func);

  public native long clCreateBuffer(long ctx, int size, int type);

  public native boolean clSetArg(long ctx, long kernel, int idx, byte[] value, int size);

  public native boolean clWriteBufferi8(long ctx, long buffer, byte[] value, int size);

  public native boolean clWriteBufferf32(long ctx, long buffer, float[] value, int size);

  public native boolean clExecute(long ctx, long kernel, int count);

  public native boolean clExecute2(long ctx, long kernel, int count1, int count2);

  public native boolean clExecute3(long ctx, long kernel, int count1, int count2, int count3);

  public native boolean clExecute4(long ctx, long kernel, int count1, int count2, int count3, int count4);

  public native boolean clReadBufferi8(long ctx, long buffer, byte[] value, int size);

  public native boolean clReadBufferf32(long ctx, long buffer, float[] value, int size);

  public native boolean clFreeKernel(long ctx, long kernel);

  public native boolean clFreeBuffer(long ctx, long buffer);

  public native boolean clClose(long ctx);
}
