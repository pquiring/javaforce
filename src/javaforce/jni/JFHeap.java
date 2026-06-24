package javaforce.jni;

/** JF Heap.
 *
 * Provides JNI API to acquire Java Heap address of arrays.
 *
 * @author pquiring
 */

public class JFHeap {
  /** Pin array and return native pointer. */
  public native static long pin(Object array);

  /** Unpin array. */
  public native static void unpin(Object array, long ptr, boolean commit);
}
