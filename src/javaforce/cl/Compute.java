package javaforce.cl;

/** Compute
 *
 * Various kernels (computations).
 *
 * @author peter.quiring
 */

import javaforce.*;

public class Compute {
  private CL cl;
  private long k_array_square;
  private long k_array_mult;
  private long k_matrix_mult;
  public boolean init(int type) {
    uninit();
    cl = CL.create(
      "kernel void array_square(global float* input, global float* output)" +
      "  {" +
      "    int i = get_global_id(0);" +
      "    output[i] = input[i] * input[i];" +
      "  };\n" +
      "kernel void array_mult(global float* input0, global float* input1, global float* output)" +
      "  {" +
      "    int i = get_global_id(0);" +
      "    output[i] = input0[i] * input1[i];" +
      "  };\n" +
      "kernel void matrix_mult(const int M, const int N, const int K, global float* A, global float* B, global float* C)" +
      "  {" +
      "    const int globalRow = get_global_id(0);" +  // row id of C (0..M)
      "    const int globalCol = get_global_id(1);" +  // col id of C (0..N)
      "    float acc = 0.0f;" +
      "    for (int k=0;k<K;k++) {" +
      "      acc += A[k*M + globalRow] * B[globalCol*K + k];" +
      "    }" +
      "    C[globalCol*M + globalRow] = acc;" +  //store result
      "  }\n"
      , type);
    k_array_square = cl.kernel("array_square");
    k_array_mult = cl.kernel("array_mult");
    k_matrix_mult = cl.kernel("matrix_mult");
    return true;
  }
  public void uninit() {
    if (cl != null) {
      cl.freeKernel(k_array_mult);
      cl.close();
      cl = null;
    }
  }

  /** array square each element.
   *
   * b[i] = a[i] * a[i];
   *
   * @param a = input array
   * @param b = output array
   *
   * All arrays must be same size.
   */

  public boolean array_square(float[] a, float[] b) {
    if (a == null) {
      JFLog.log("Compute:array_square:a invalid");
      return false;
    }
    int size = a.length;
    if (b == null || b.length != size) {
      JFLog.log("Compute:array_square:b invalid");
      return false;
    }
    long input0 = cl.createWriteBuffer(Float.BYTES * size);
    long output = cl.createReadBuffer(Float.BYTES * size);
    cl.writeBuffer(input0, a);
    cl.setArg(k_array_square, 0, input0);
    cl.setArg(k_array_square, 1, output);
    cl.execute(k_array_square, size);
    cl.readBuffer(output, b);
    cl.freeBuffer(input0);
    cl.freeBuffer(output);
    return true;
  }

  /** array multiple.
   *
   * c[i] = x[i] * y[i];
   *
   * @param a = input array
   * @param b = input array
   * @param c = output array
   *
   * All arrays must be same size.
   *
   */
  public boolean array_mult(float[] a, float[] b, float[] c) {
    if (a == null) {
      JFLog.log("Compute:array_mult:a invalid");
      return false;
    }
    int size = a.length;
    if (b == null || b.length != size) {
      JFLog.log("Compute:array_mult:b invalid");
      return false;
    }
    if (c == null || c.length != size) {
      JFLog.log("Compute:array_mult:c invalid");
      return false;
    }
    long input0 = cl.createWriteBuffer(Float.BYTES * size);
    long input1 = cl.createWriteBuffer(Float.BYTES * size);
    long output = cl.createReadBuffer(Float.BYTES * size);
    cl.writeBuffer(input0, a);
    cl.writeBuffer(input1, b);
    cl.setArg(k_array_mult, 0, input0);
    cl.setArg(k_array_mult, 1, input1);
    cl.setArg(k_array_mult, 2, output);
    cl.execute(k_array_mult, size);
    cl.readBuffer(output, c);
    cl.freeBuffer(input0);
    cl.freeBuffer(input1);
    cl.freeBuffer(output);
    return true;
  }

  /** matrix multiple.
   *
   *   c = ab
   *
   * https://cnugteren.github.io/tutorial/pages/page1.html
   *
   *          N
   *         ...
   *       K .B.
   *         ...
   *    K
   *   ...   ...
   * M .A.   .C.
   *   ...   ...
   *
   * @param as = a size
   * @param bs = b size
   * @param ks = common size
   * @param a = input array
   * @param b = input array
   * @param c = output array
   */
  public boolean matrix_mult(int as, int bs, int ks, float[] a, float[] b, float[] c) {
    int a_size = as * ks;
    if (a == null || a.length != a_size) {
      JFLog.log("Compute:matrix_mult:a invalid");
      return false;
    }
    int b_size = bs * ks;
    if (b == null || b.length != b_size) {
      JFLog.log("Compute:matrix_mult:b invalid");
      return false;
    }
    int c_size = as * bs;
    if (c == null || c.length != c_size) {
      JFLog.log("Compute:matrix_mult:c invalid");
      return false;
    }
    long input0 = cl.createWriteBuffer(Float.BYTES * a_size);
    long input1 = cl.createWriteBuffer(Float.BYTES * b_size);
    long output = cl.createReadBuffer(Float.BYTES * c_size);
    cl.writeBuffer(input0, a);
    cl.writeBuffer(input1, b);
    cl.setArg(k_matrix_mult, 0, as);  //M
    cl.setArg(k_matrix_mult, 1, bs);  //N
    cl.setArg(k_matrix_mult, 2, ks);  //K
    cl.setArg(k_matrix_mult, 3, input0);  //A
    cl.setArg(k_matrix_mult, 4, input1);  //B
    cl.setArg(k_matrix_mult, 5, output);  //C
    cl.execute2(k_matrix_mult, as, bs);
    cl.readBuffer(output, c);
    cl.freeBuffer(input0);
    cl.freeBuffer(input1);
    cl.freeBuffer(output);
    return true;
  }
}
