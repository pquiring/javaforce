package javaforce;

/** JF Array generic
 *
 * T must be Object (not primitive)
 *
 * @author pquiring
 */

import java.util.*;

public abstract class JFArray<T> {
  protected T[] buf;
  protected int count;

  public static int initSize = 64;

  public JFArray() {
    count = 0;
    alloc(initSize);
  }

  public abstract void alloc(int size);

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
  }

  public void append(T s) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    buf[count] = s;
    count = newcount;
  }

  public void append(T[] s) {
    int newcount = count + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, count, s.length);
    count = newcount;
  }

  public void set(T[] s, int pos) {
    int newcount = pos + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, pos, s.length);
  }

  public T[] toArray() {
    return Arrays.copyOf(buf, count);
  }

  public T[] toArray(int pos, int length) {
    return Arrays.copyOfRange(buf, pos, pos + length);
  }

  //returns the backing buffer (size may be larger than expected)
  public T[] getBuffer() {
    return buf;
  }
}

