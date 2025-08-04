package javaforce.ansi.client;

/**
 * Line.java
 *
 * Created on August 2, 2007, 8:06 PM
 *
 * @author pquiring
 */

import java.util.*;

public class Line {

  public Line(int len, int fc, int bc) {
    this.len = len;
    chs = new char[len];
    fcs = new int[len];
    bcs = new int[len];
    blinks = new boolean[len];
    clear(fc, bc);
    dirty = true;
  }

  public int len;
  public char[] chs;
  public int[] fcs;  //fore color
  public int[] bcs;  //back color
  public boolean[] blinks;
  public boolean dirty;
  public int y;

  public void clear(int fc, int bc) {
    for(int x=0;x<len;x++) {
      chs[x] = ' ';
      fcs[x] = fc;
      bcs[x] = bc;
      blinks[x] = false;
    }
  }

  public void set(int x, int fc, int bc) {
    chs[x] = ' ';
    fcs[x] = fc;
    bcs[x] = bc;
    blinks[x] = false;
  }

  public void copy(int dst, int src) {
    chs[dst] = chs[src];
  }

  public void setlen(int len, int fc, int bc) {
    int oldlen = this.len;
    if (oldlen == len) return;
    chs = Arrays.copyOf(chs, len);
    fcs = Arrays.copyOf(fcs, len);
    bcs = Arrays.copyOf(bcs, len);
    blinks = Arrays.copyOf(blinks, len);
    for(int i = oldlen;i<len;i++) {
      chs[i] = ' ';
      fcs[i] = fc;
      bcs[i] = bc;
      blinks[i] = false;
    }
    this.len = len;
    dirty = true;
  }
}
