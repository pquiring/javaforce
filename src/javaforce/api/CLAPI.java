package javaforce.api;

import javaforce.ffm.*;
import javaforce.jni.*;

/** OpenCL native API.
 *
 * @author pquiring
 */

public interface CLAPI {

  public static CLAPI getInstance() {
    if (FFM.enabled()) {
      return CLFFM.getInstance();
    } else {
      return CLJNI.getInstance();
    }
  }

  public boolean clLoadLibrary(String file);
  public long clCreate(String src, int type);
  public long clKernel(long ctx, String func);
  public long clCreateBuffer(long ctx, int size, int type);
  public boolean clSetArg(long ctx, long kernel, int idx, byte[] value, int size);
  public boolean clWriteBufferi8(long ctx, long buffer, byte[] value, int size);
  public boolean clWriteBufferf32(long ctx, long buffer, float[] value, int size);
  public boolean clExecute(long ctx, long kernel, int count);
  public boolean clExecute2(long ctx, long kernel, int count1, int count2);
  public boolean clExecute3(long ctx, long kernel, int count1, int count2, int count3);
  public boolean clExecute4(long ctx, long kernel, int count1, int count2, int count3, int count4);
  public boolean clReadBufferi8(long ctx, long buffer, byte[] value, int size);
  public boolean clReadBufferf32(long ctx, long buffer, float[] value, int size);
  public boolean clFreeKernel(long ctx, long kernel);
  public boolean clFreeBuffer(long ctx, long buffer);
  public boolean clClose(long ctx);
}
