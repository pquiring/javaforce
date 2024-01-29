package javaforce.media;

/**
 * FIFO buffer for audio samples (16bit only).
 * Uses a cyclical buffer to avoid using locks or reallocation.
 */

import javaforce.*;

public class AudioBuffer {

  private int bufsiz;
  private short[] buf;
  private int head = 0, tail = 0;

  public AudioBuffer(int freq, int chs, int seconds) {
    bufsiz = freq * chs * seconds;
    buf = new short[bufsiz];
    clear();
  }

  public void add(short[] in, int pos, int len) {
    boolean wrap = false;
    int nhead = head + len;
    if (nhead >= bufsiz) {
      wrap = true;
      nhead -= bufsiz;
    }
    if (nhead == tail) return;  //buffer full
    if ( (head > tail) && (nhead > tail) && (wrap) ) return;  //buffer full
    if ( (head < tail) && (nhead > tail) && (!wrap) ) return;  //buffer full
    if (wrap) {
      //copy 2 pieces
      int p1 = bufsiz - head;
      int p2 = len - p1;
      System.arraycopy(in, pos, buf, head, p1);
      System.arraycopy(in, pos+p1, buf, 0, p2);
    } else {
      //copy 1 piece
      System.arraycopy(in, pos, buf, head, len);
    }
    head = nhead;
  }

  public boolean get(short[] out, int pos, int len) {
    if (tail == head) return false;
    boolean wrap = false;
    int ntail = tail + len;
    if (ntail >= bufsiz) {
      wrap = true;
      ntail -= bufsiz;
    }
    if ( (tail > head) && (ntail > head) && (wrap) ) return false;  //buffer empty
    if ( (tail < head) && (ntail > head) && (!wrap) ) return false;  //buffer empty
    if (wrap) {
      //copy 2 pieces
      int p1 = bufsiz - tail;
      int p2 = len - p1;
      System.arraycopy(buf, tail, out, pos, p1);
      System.arraycopy(buf, 0, out, pos+p1, p2);
    } else {
      //copy 1 piece
      System.arraycopy(buf, tail, out, pos, len);
    }
    tail = ntail;
    return true;
  }

  /** Returns # samples in buffer */
  public int size() {
    int t = tail;
    int h = head;
    if (t == h) return 0;
    if (h > t) return h - t;
    return bufsiz - t + h;
  }

  public void clear() {
    tail = head;
  }
}
