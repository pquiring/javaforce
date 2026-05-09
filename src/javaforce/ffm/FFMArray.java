package javaforce.ffm;

/** FFMArray
 *
 * Array Factory used by native C code to create Java arrays.
 *
 * Also serves as a cache for arrays.
 *
 * @author pquiring
 */

import java.lang.foreign.*;

import javaforce.*;
import javaforce.jni.*;

public class FFMArray {
  private Object ref;
  private long ptr;

  public long pin() {
    ptr = JFNative.pin(ref);
    return ptr;
  }

  public void unpin() {
    JFNative.unpin(ref, ptr, true);
    ptr = 0;
  }

  private byte[] ba;
  public long NewByteArray(int size) {
    if (ptr != 0) unpin();
    if (ba == null || ba.length != size) {
      ba = new byte[size];
    }
    ref = ba;
    return pin();
  }

  private short[] sa;
  public long NewShortArray(int size) {
    if (ptr != 0) unpin();
    if (sa == null || sa.length != size) {
      sa = new short[size];
    }
    ref = sa;
    return pin();
  }

  private int[] ia;
  public long NewIntArray(int size) {
    if (ptr != 0) unpin();
    if (ia == null || ia.length != size) {
      ia = new int[size];
    }
    ref = ia;
    return pin();
  }

  private long[] la;
  public long NewLongArray(int size) {
    if (ptr != 0) unpin();
    if (la == null || la.length != size) {
      la = new long[size];
    }
    ref = la;
    return pin();
  }

  private float[] fa;
  public long NewFloatArray(int size) {
    if (ptr != 0) unpin();
    if (fa == null || fa.length != size) {
      fa = new float[size];
    }
    ref = fa;
    return pin();
  }

  private String[] Sa;
  public long NewStringArray(int size) {
    if (ptr != 0) unpin();
    //always create new String array
    Sa = new String[size];
    ref = Sa;
    return pin();
  }

  public void SetStringElement(int idx, MemorySegment str) {
    if (ref == null) return;
    if (idx < 0 || idx >= Sa.length) return;
    Sa[idx] = FFM.getString(str);
  }

  public Object getArray() {
    if (ref == null) return null;
    if (ptr != 0) unpin();
    Object array = ref;
    ref = null;
    return array;
  }
}
