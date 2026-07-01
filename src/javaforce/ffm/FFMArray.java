package javaforce.ffm;

import java.lang.foreign.*;

import javaforce.*;
import javaforce.jni.*;

/** FFMArray.
 *
 * Array Factory used by native C code to create Java arrays.
 *
 * Also serves as a cache for arrays.
 *
 * @author pquiring
 */

public class FFMArray {
  private Object obj;
  private long ptr;
  private long ref;
  private boolean string2;

  public long pin() {
    ptr = JFHeap.pin(obj);
    return ptr;
  }

  public void unpin() {
    JFHeap.unpin(obj, ptr, true);
    ptr = 0;
  }

  public long ref() {
    ref = JFHeap.ref(obj);
    return ref;
  }

  public void unref() {
    JFHeap.unref(ref);
  }

  private byte[] ba;
  public long NewByteArray(int size) {
    if (ptr != 0) unpin();
    if (ba == null || ba.length != size) {
      ba = new byte[size];
    }
    obj = ba;
    return pin();
  }

  private short[] sa;
  public long NewShortArray(int size) {
    if (ptr != 0) unpin();
    if (sa == null || sa.length != size) {
      sa = new short[size];
    }
    obj = sa;
    return pin();
  }

  private int[] ia;
  public long NewIntArray(int size) {
    if (ptr != 0) unpin();
    if (ia == null || ia.length != size) {
      ia = new int[size];
    }
    obj = ia;
    return pin();
  }

  private long[] la;
  public long NewLongArray(int size) {
    if (ptr != 0) unpin();
    if (la == null || la.length != size) {
      la = new long[size];
    }
    obj = la;
    return pin();
  }

  private float[] fa;
  public long NewFloatArray(int size) {
    if (ptr != 0) unpin();
    if (fa == null || fa.length != size) {
      fa = new float[size];
    }
    obj = fa;
    return pin();
  }

  private double[] da;
  public long NewDoubleArray(int size) {
    if (ptr != 0) unpin();
    if (da == null || da.length != size) {
      da = new double[size];
    }
    obj = da;
    return pin();
  }

  private String[] Sa;
  public long NewStringArray(int size) {
    if (ptr != 0) unpin();
    //always create new String array
    Sa = new String[size];
    if (!string2) {
      obj = Sa;
    } else {
      String[][] org = Sa2;
      Sa2 = new String[org.length + 1][];
      System.arraycopy(org, 0, Sa2, 0, org.length);
      Sa2[org.length] = Sa;
    }
    return ref();
  }

  public void SetStringElement(int idx, MemorySegment str) {
    if (obj == null) return;
    if (idx < 0 || idx >= Sa.length) return;
    Sa[idx] = FFM.getString(str);
  }

  private String[][] Sa2;
  /** To create a String[][] call this method first and each time NewStringArray() is called it will be appended to this String[][] array. */
  public long NewString2Array() {
    if (ptr != 0) unpin();
    //always create new String array
    Sa2 = new String[0][];
    obj = Sa2;
    string2 = true;
    return ref();
  }

  public Object getArray() {
    if (obj == null) return null;
    if (ptr != 0) unpin();
    Object array = obj;
    obj = null;
    string2 = false;
    return array;
  }
}
