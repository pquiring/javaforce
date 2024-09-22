package javaforce.cl;

/** Compute (WIP)
 *
 * @author peter.quiring
 */

public class Compute {
  private CL cl;
  private long mult;
  public void init(int type) {
    uninit();
    cl = CL.create(
      "__kernel void mult(__global float* input0, __global float* input1, __global float* output-) { int i = get_global_id(0); output[i] = input0[i] * input1[i]; }",
      type);
    mult = cl.kernel("mult");
  }
  public void uninit() {
    if (cl != null) {
      cl.freeKernel(mult);
      cl.close();
      cl = null;
    }
  }
  public boolean mult(float[] x, float[] y, float[] res) {
    int size = x.length;
    if (y.length != size) return false;
    if (res.length != size) return false;
    long input0 = cl.createWriteBuffer(Float.BYTES * size);
    long input1 = cl.createWriteBuffer(Float.BYTES * size);
    long output = cl.createReadBuffer(Float.BYTES * size);
    cl.writeBuffer(input0, x);
    cl.writeBuffer(input1, y);
    cl.setArg(mult, 0, input0);
    cl.setArg(mult, 1, input1);
    cl.setArg(mult, 2, output);
    cl.execute(mult, size);
    cl.readBuffer(output, res);
    cl.freeBuffer(input0);
    cl.freeBuffer(input1);
    cl.freeBuffer(output);
    return true;
  }
}
