package javaforce;

/**
 *
 * @author pquiring
 *
 * Created : Sept 17, 2013
 */

import java.util.*;

public class JFArrayInt {
  private int[] buf;
  private int count;

  public static int initSize = 64;

  public JFArrayInt() {
    count = 0;
    buf = new int[initSize];
  }

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
    if (buf.length != initSize) buf = new int[initSize];
  }

  public void append(int s) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    buf[count] = s;
    count = newcount;
  }

  public void append(int[] s) {
    int newcount = count + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, count, s.length);
    count = newcount;
  }

  public int[] toArray() {
    return Arrays.copyOf(buf, count);
  }

  //returns the backing buffer (size may be larger than expected)
  public int[] getBuffer() {
    return buf;
  }
}
