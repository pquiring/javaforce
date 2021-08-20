package javaforce;

/**
 *
 * @author pquiring
 *
 * Created : Sept 17, 2013
 */

import java.util.*;

public class JFArrayByte extends JFArray<byte[]> {
  private byte buf[];
  private int count;

  public static int initSize = 64;

  public JFArrayByte() {
    count = 0;
    buf = new byte[initSize];
    obtainPointer();
  }

  public int size() {
    return count;
  }

  public void clear() {
    count = 0;
    if (buf.length != initSize) {
      buf = new byte[initSize];
      updatePointer();
    }
  }

  public void append(byte s) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
      updatePointer();
    }
    buf[count] = s;
    count = newcount;
  }

  public void append(byte s[]) {
    int newcount = count + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
      updatePointer();
    }
    System.arraycopy(s, 0, buf, count, s.length);
    count = newcount;
  }

  public void set(byte s[], int pos) {
    int newcount = pos + s.length;
    if (newcount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
      updatePointer();
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

  /*
    JFArray stress test

  GC1 : Fails after 1 min (deadlock)
  Z : Fails after 1 min (deadlock)
  Shenandoah : Fails after 6 mins (OutOfMemory exception)
  OpenJ9 : Fails after 1 min (exceptions)
  GraalVM : Fails after 6 mins (OutOfMemory)
  jfdk.sf.net : Passes

  */
  public static void main(String[] args) {
    javaforce.jni.JFNative.load();
    System.out.println("isGraal = " + JF.isGraal());
    JFArrayByte[] arrs = new JFArrayByte[1024];
    Random r = new Random();
    while (true) {
      int idx = r.nextInt(1024);
      int size = r.nextInt(1024);
      arrs[idx] = new JFArrayByte();
      arrs[idx].append(new byte[size]);
      System.out.println("pointer = 0x" + Long.toString(arrs[idx].getPointer(), 16));
    }
  }
}
