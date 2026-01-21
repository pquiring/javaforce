package javaforce.cl;

/** OpenCL
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public interface CL {

  public static final int TYPE_DEFAULT = (1 << 0);
  public static final int TYPE_CPU = (1 << 1);
  public static final int TYPE_GPU = (1 << 2);
  public static final int TYPE_ACCELERATOR = (1 << 3);

  public static CL getInstance() {
    return CLJNI.getInstance();
  }

  public long create(String src, int type);
  public long create(String src);

  public long kernel(long ctx, String func);

  public static final int MEM_READ_WRITE = (1 << 0);
  public static final int MEM_WRITE = (1 << 1);
  public static final int MEM_READ = (1 << 2);

  public long createReadBuffer(long ctx, int size);
  public long createWriteBuffer(long ctx, int size);
  public long createReadWriteBuffer(long ctx, int size);

  public boolean setArg(long ctx, long kernel, int idx, byte[] value);

  public boolean setArg(long ctx, long kernel, int idx, int value);

  public boolean setArg(long ctx, long kernel, int idx, long value);

  public boolean writeBuffer(long ctx, long buffer, byte[] data);

  public boolean writeBuffer(long ctx, long buffer, float[] data);

  public boolean execute(long ctx, long kernel, int count);

  public boolean execute2(long ctx, long kernel, int count1, int count2);

  public boolean execute3(long ctx, long kernel, int count1, int count2, int count3);

  public boolean execute4(long ctx, long kernel, int count1, int count2, int count3, int count4);

  public boolean readBuffer(long ctx, long buffer, byte[] data);

  public boolean readBuffer(long ctx, long buffer, float[] data);

  public boolean freeKernel(long ctx, long kernel);

  public boolean freeBuffer(long ctx, long buffer);

  public boolean close(long ctx);

  public static void main(String[] args) {
    Compute compute = new Compute();
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
      SIZE = 16;
      float[] a = new float[SIZE];
      for(int i=0;i<SIZE;i++) {
        a[i] = rand.nextFloat();
      }
      float[] b = new float[SIZE];

      compute.array_square(a, b);

      //confirm results
      int correct = 0;
      for(int i=0;i<SIZE;i++) {
        float res = a[i] * a[i];
        if (b[i] == res) {
          correct++;
        } else {
          JFLog.log("error:b[]=" + b[i] + ",expected=" + res);
        }
      }

      System.out.println("array_square:" + correct + " of " + SIZE + " are correct");
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
            JFLog.log("error:c[] = " + c[i] + ":expected=" + res);
          }
        }
      }
      if (true) {
        javaforce.Console.printArray(a);
        javaforce.Console.printArray(b);
        javaforce.Console.printArray(c);
      }
      System.out.println("matrix_mult:" + correct + " of " + SIZE_SIZE + " are correct");
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
