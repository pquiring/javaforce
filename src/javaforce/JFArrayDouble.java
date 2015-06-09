package javaforce;

/**
 *
 * @author pquiring
 *
 * Created : May 4, 2015
 */

import java.util.*;

public class JFArrayDouble {
  private double buf[];
  private int count;

  public static int initSize = 64;

  public JFArrayDouble() {
    count = 0;
    buf = new double[initSize];
  }

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
    if (buf.length != initSize) buf = new double[initSize];
  }

  public void append(double f) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    buf[count] = f;
    count = newcount;
  }

  public void append(double f[]) {
    int newcount = count + f.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(f, 0, buf, count, f.length);
    count = newcount;
  }

  public double[] toArray() {
    return Arrays.copyOf(buf, count);
  }

  //returns the backing buffer (size may be larger than expected)
  public double[] getBuffer() {
    return buf;
  }
}
