package javaforce.tests;

/** Test OpenCL
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.cl.*;
import static javaforce.cl.CL.*;

public class TestCL {
  public static void main(String[] args) {
    System.out.println("TestCL");
    int SIZE;
    int SIZE_SIZE;
    Random rand = new Random();
    Compute compute = null;
    try {
      compute = new Compute();
      if (!compute.init(TYPE_GPU)) {
        JFLog.log("Compute init failed");
        System.exit(1);
      }
      System.out.println("Starting tests...");
    } catch (Throwable t) {
      JFLog.log(t);
    }
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
