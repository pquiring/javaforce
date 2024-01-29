package javaforce;

/**
 *
 * @author pquiring
 *
 * Created : Sept 17, 2013
 */

import java.util.*;

public class JFArrayByte {
  private byte[] buf;
  private int count;

  public static int initSize = 64;

  public JFArrayByte() {
    count = 0;
    buf = new byte[initSize];
  }

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
    if (buf.length != initSize) buf = new byte[initSize];
  }

  public void append(byte s) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    buf[count] = s;
    count = newcount;
  }

  public void append(byte[] s) {
    int newcount = count + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, count, s.length);
    count = newcount;
  }

  public void set(byte[] s, int pos) {
    int newcount = pos + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    }
    System.arraycopy(s, 0, buf, pos, s.length);
  }

  public byte[] toArray() {
    return Arrays.copyOf(buf, count);
  }

  public byte[] toArray(int pos, int length) {
    return Arrays.copyOfRange(buf, pos, pos + length);
  }

  //returns the backing buffer (size may be larger than expected)
  public byte[] getBuffer() {
    return buf;
  }
}
