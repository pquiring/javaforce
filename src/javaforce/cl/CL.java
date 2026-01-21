package javaforce.cl;

/** OpenCL
 *
 * @author peter.quiring
 */

import javaforce.jni.*;

public interface CL {

  public static final int TYPE_DEFAULT = (1 << 0);
  public static final int TYPE_CPU = (1 << 1);
  public static final int TYPE_GPU = (1 << 2);
  public static final int TYPE_ACCELERATOR = (1 << 3);

  public static CL getInstance() {
    return CLJNI.getInstance();
  }

  /** Create OpenGL Context with source code.
   * close() context to release it.
   *
   * @param src = OpenCL source code
   * @param type = TYPE_...
   */
  public long create(String src, int type);

  /** Create OpenGL Context (type = TYPE_DEFAULT).
   * close() context to release it.
   *
   * @param src = OpenCL source code
   */
  public long create(String src);

  /** Create OpenGL Kernel.
   * freeKernel() to release it.
   *
   * @param ctx = OpenCL Context
   * @param func = function name
   */
  public long kernel(long ctx, String func);

  public static final int MEM_READ_WRITE = (1 << 0);
  public static final int MEM_WRITE = (1 << 1);
  public static final int MEM_READ = (1 << 2);

  /** Create a read-only buffer. */
  public long createReadBuffer(long ctx, int size);
  /** Create a write-only buffer. */
  public long createWriteBuffer(long ctx, int size);
  /** Create a read-write buffer. */
  public long createReadWriteBuffer(long ctx, int size);

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, byte[] value);

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, int value);

  /** Set a kernel parameter. */
  public boolean setArg(long ctx, long kernel, int idx, long value);

  /** Write to a buffer. */
  public boolean writeBuffer(long ctx, long buffer, byte[] data);

  /** Write to a buffer. */
  public boolean writeBuffer(long ctx, long buffer, float[] data);

  /** Execute a kernel with one counter. */
  public boolean execute(long ctx, long kernel, int count);

  /** Execute a kernel with two counters. */
  public boolean execute2(long ctx, long kernel, int count1, int count2);

  /** Execute a kernel with three counters. */
  public boolean execute3(long ctx, long kernel, int count1, int count2, int count3);

  /** Execute a kernel with four counters. */
  public boolean execute4(long ctx, long kernel, int count1, int count2, int count3, int count4);

  /** Read from a buffer. */
  public boolean readBuffer(long ctx, long buffer, byte[] data);

  /** Read from a buffer. */
  public boolean readBuffer(long ctx, long buffer, float[] data);

  /** Release a kernel. */
  public boolean freeKernel(long ctx, long kernel);

  /** Release a buffer. */
  public boolean freeBuffer(long ctx, long buffer);

  /** Release an OpenCL context. */
  public boolean close(long ctx);
}
