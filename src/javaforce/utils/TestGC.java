package javaforce.utils;

/** TestGC
 *
 * Uses 1GB memory with very high turn over, run with max of 8GB.
 *
 * GC Options:
 *
 * -XX:+UseG1GC                 #default
 * -XX:+UseZGC                  #fast (JDK15) (JEP377)
 *   -XX:-ZUncommit               #disable uncommit
 *   -XX:+ZGenerational           #enable generational collection (JDK21) (JEP439)
 * -XX:+UseShenandoahGC         #RedHat only
 * -XX:+UseSerialGC             #slow
 * -XX:+UseParallelGC           #slow
 * -XX:+UseEpsilonGC            #no-op
 *
 * Execute:
 * bin\jfexecd -Xmx8G -XX:+UseZGC -XX:-ZUncommit -cp javaforce.jar javaforce.utils.TestGC
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public class TestGC extends Thread {
  public static int threadCount = 64;
  public static void main(String[] args) {
    TestGC[] threads = new TestGC[threadCount];
    JF.sleep(3000);
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
    Random r = new Random();
    while (true) {
      JF.sleep(50);
      int size = 16 * 1024 * 1024 + r.nextInt(1024);
      WinNative.hold(new int[size], 50);
    }
  }
}
