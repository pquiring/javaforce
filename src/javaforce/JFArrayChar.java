package javaforce;

/**
 *
 * @author pquiring
 *
 * Created : May 4, 2015
 */

import java.util.*;

public class JFArrayChar {
  private char[] buf;
  private int count;

  public static int initSize = 64;

  public JFArrayChar() {
    count = 0;
    buf = new char[initSize];
  }

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
    if (buf.length != initSize) buf = new char[initSize];
  }

  public void append(char f) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    buf[count] = f;
    count = newcount;
  }

  public void append(char[] f) {
    int newcount = count + f.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(f, 0, buf, count, f.length);
    count = newcount;
  }

  public void set(char[] s, int pos) {
    int newcount = pos + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, pos, s.length);
  }

  public char[] toArray() {
    return Arrays.copyOf(buf, count);
  }

  public char[] toArray(int pos, int length) {
    return Arrays.copyOfRange(buf, pos, pos + length);
  }

  //returns the backing buffer (size may be larger than expected)
  public char[] getBuffer() {
    return buf;
  }
}
