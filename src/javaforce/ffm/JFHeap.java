package javaforce.ffm;

/** JF Heap.
 *
 * Provides JNI API to acquire Java Heap address of arrays.
 * And object references.
 *
 * @author pquiring
 */

public class JFHeap {
  /** Pin array and return native pointer. */
  public native static long pin(Object array);

  /** Unpin array. */
  public native static void unpin(Object array, long ptr, boolean commit);

  /** Return jni reference to object. */
  public native static long ref(Object obj);

  /** Release jni reference to object. */
  public native static void unref(long ref);
}
