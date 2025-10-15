package javaforce.tests;

/**
 *
 * @author User
 */

import javaforce.gl.*;

public class TestMath {
  private static void print(Matrix mat) {
    for(int a=0;a<16;a++) {
      if (a % 4 == 0) System.out.println("");
      System.out.print(Float.toString(mat.m[a]));
      System.out.print(",");
    }
  }
  public static void main(String[] args) {
    Matrix mat = new Matrix();
    mat.setAA(90, 1, 0, 0);
    print(mat);
    mat.setIdentity();
    mat.addRotate(90, 1, 0, 0);
    System.out.println("\r\n====================");
    print(mat);
  }

}
