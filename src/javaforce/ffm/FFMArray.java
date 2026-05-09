package javaforce.ffm;

/** FFMArray
 *
 * Used to create Java array for native functions that return an array.
 *
 * @author pquiring
 */

import java.lang.foreign.*;

import javaforce.*;
import javaforce.jni.*;

public class FFMArray {
  private Object ref;
  private long ptr;
  private Arena arena;

  private long pin() {
    ptr = JFNative.pin(ref);
    return ptr;
  }

  private void unpin() {
    JFNative.unpin(ref, ptr, true);
    ptr = 0;
  }

  public long NewByteArray(int size) {
    if (ptr != 0) unpin();
    ref = new byte[size];
    return pin();
  }

  public long NewShortArray(int size) {
    if (ptr != 0) unpin();
    ref = new short[size];
    return pin();
  }

  public long NewIntArray(int size) {
    if (ptr != 0) unpin();
    ref = new int[size];
    return pin();
  }

  public long NewLongArray(int size) {
    if (ptr != 0) unpin();
    ref = new long[size];
    return pin();
  }

  public long NewFloatArray(int size) {
    if (ptr != 0) unpin();
    ref = new float[size];
    return pin();
  }

  public long NewStringArray(int size) {
    if (ptr != 0) unpin();
    ref = new String[size];
    return pin();
  }

  public void SetStringElement(int idx, MemorySegment str) {
    if (ref == null) return;
    String[] strs = (String[])ref;
    if (idx < 0 || idx >= strs.length) return;
    strs[idx] = FFM.getString(str);
  }

  public Object getArray() {
    if (ref == null) return null;
    if (ptr != 0) unpin();
    Object array = ref;
    ref = null;
    return array;
  }

  /** getUpcall() creates C up-call function to allocate Java Arrays.
   * Called from JNIArray (see native/common/array.h)
   */
  public long getUpcall(String type) {
    FFM ffm = FFM.getInstanceJNI();  //to call setupUpcalls()
    switch (type) {
      case "Byte": return FFM.upcall_FFMArray_NewByteArray.address();
      case "Short": return FFM.upcall_FFMArray_NewShortArray.address();
      case "Int": return FFM.upcall_FFMArray_NewIntArray.address();
      case "Long": return FFM.upcall_FFMArray_NewLongArray.address();
      case "Float": return FFM.upcall_FFMArray_NewFloatArray.address();
      case "String": return FFM.upcall_FFMArray_NewStringArray.address();
    }
    return 0;
  }
}
