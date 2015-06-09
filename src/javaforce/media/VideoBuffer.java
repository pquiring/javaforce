package javaforce.media;

/** FIFO buffer for video frames (images)
 * Uses a cyclical buffer to avoid using locks or reallocation.
 *
 * @author pquiring
 *
 * Created : Oct 24, 2013
 */

import javaforce.*;

public class VideoBuffer {
  private JFImage images[];
  private int head = 0, tail = 0;
  private boolean headInUse, tailInUse;

  public VideoBuffer(int width, int height, int frames) {
    images = new JFImage[frames];
    for(int a=0;a<frames;a++) {
      images[a] = new JFImage(width, height);
    }
  }

  //head functions

  /** Returns the next image to add a frame to the buffer (the head)
   * Returns null if buffer is full.
   */
  public JFImage getNewFrame() {
    if (headInUse) freeNewFrame();
    int newHead = head + 1;
    if (newHead == images.length) newHead = 0;
    if (newHead == tail) return null;  //buffer full
    headInUse = true;
    return images[head];
  }

  /** Frees the image obtained by getNewFrame() */
  public void freeNewFrame() {
    if (!headInUse) return;
    headInUse = false;
    head++;
    if (head == images.length) head = 0;
  }

  //tail functions

  /** Returns the next image to be removed from the buffer (the tail)*/
  public JFImage getNextFrame() {
    if (tailInUse) freeNextFrame();
    if (tail == head) return null;
    tailInUse = true;
    return images[tail];
  }

  /** Frees the image obtained by getNextFrame() */
  public void freeNextFrame() {
    if (!tailInUse) return;
    tailInUse = false;
    tail++;
    if (tail == images.length) tail = 0;
  }

  public int size() {
    int t = tail;
    int h = head;
    if (t == h) return 0;
    if (h > t) return h - t;
    return images.length - t + h;
  }

  public void clear() {
    tail = head;
  }
}
