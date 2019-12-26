package javaforce.utils;

/** TestGC
 *
 * @author pquiring
 */

import javaforce.jni.*;

public class TestGC extends Thread {
  public static int threadCount = 20;
  public static void main(String args[]) {
    TestGC threads[] = new TestGC[threadCount];
    for(int a=0;a<threadCount;a++) {
      threads[a] = new TestGC();
      threads[a].start();
    }
    System.out.println("Press ENTER to quit");
    try {
      int in = System.in.read();
    } catch (Exception e) {}
    System.exit(1);
  }
  public void run() {
    while (true) {
      WinNative.hold(new int[1024 * 1024], 10);
    }
  }
}
