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
    return JFNative.pin(ref);
  }

  public long NewByteArray(int size) {
    ref = new byte[size];
    return pin();
  }

  public long NewShortArray(int size) {
    ref = new short[size];
    return pin();
  }

  public long NewIntArray(int size) {
    ref = new int[size];
    return pin();
  }

  public long NewLongArray(int size) {
    ref = new long[size];
    return pin();
  }

  public long NewFloatArray(int size) {
    ref = new float[size];
    return pin();
  }

  public long NewStringArray(int size) {
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
   if (ptr != 0) {
     JFNative.unpin(ref, ptr, true);
     ptr = 0;
   }
   return ref;
  }

  /** getUpcall() creates C up-call function to allocate Java Arrays. */
  public MemorySegment getUpcall(FFM ffm, Arena arena, String type) {
    if (type.equals("String")) {
      return FFM.toMemory(arena, new MemorySegment[] {
        ffm.getFunctionUpCall(this, "NewStringArray", long.class, new Class[] {int.class}, arena),
        ffm.getFunctionUpCall(this, "SetStringElement", void.class, new Class[] {int.class, MemorySegment.class}, arena),
      });
    } else {
      return ffm.getFunctionUpCall(this, "New" + type + "Array", long.class, new Class[] {int.class}, arena);
    }
  }

  /** getUpcall() creates C up-call function to allocate Java Arrays.
   * This variant used by JNIArray
   */
  public long getUpcall(String type) {
    long addr = 0;
    try {
      arena = Arena.ofAuto();
      addr = getUpcall(FFM.getInstanceJNI(), arena, type).address();
    } catch (Exception e) {
      JFLog.log(e);
    }
    return addr;
  }
}
