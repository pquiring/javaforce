package javaforce.api;

/** OpenCL native API
 *
 * @author pquiring
 */

public interface CLAPI {
  public boolean clLoadLibrary(String lib);
  public long clCreate(String src, int type);
  public long clKernel(long ctx, String func);
  public long clCreateBuffer(long ctx, int size, int type);
  public boolean clSetArg(long ctx, long kernel, int idx, byte[] value, int len);
  public boolean clWriteBufferi8(long ctx, long buffer, byte[] value, int len);
  public boolean clWriteBufferf32(long ctx, long buffer, float[] value, int len);
  public boolean clExecute(long ctx, long kernel, int count);
  public boolean clExecute2(long ctx, long kernel, int count1, int count2);
  public boolean clExecute3(long ctx, long kernel, int count1, int count2, int count3);
  public boolean clExecute4(long ctx, long kernel, int count1, int count2, int count3, int count4);
  public boolean clReadBufferi8(long ctx, long buffer, byte[] data, int len);
  public boolean clReadBufferf32(long ctx, long buffer, float[] data, int len);
  public boolean clFreeKernel(long ctx, long kernel);
  public boolean clFreeBuffer(long ctx, long buffer);
  public boolean clClose(long ctx);
}
