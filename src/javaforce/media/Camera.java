package javaforce.media;

/**
 * Web Camera API
 *
 * @author pquiring
 *
 * Created : Aug 20, 2013
 */

public class Camera {
  private long ctx = 0;
  private int[] mirror;
  private int[] flip;

  private native long cameraInit();
  private native boolean cameraUninit(long ctx);
  private native String[] cameraListDevices(long ctx);
  private native String[] cameraListModes(long ctx, int deviceIdx);
  private native boolean cameraStart(long ctx, int deviceIdx, int width, int height);
  private native boolean cameraStop(long ctx);
  private native int[] cameraGetFrame(long ctx);
  private native int cameraGetWidth(long ctx);
  private native int cameraGetHeight(long ctx);

  public boolean init() {
    ctx = cameraInit();
    return ctx != 0;
  }

  public boolean uninit() {
    return cameraUninit(ctx);
  }

  public String[] listDevices() {
    return cameraListDevices(ctx);
  }

  public String[] listModes(int deviceIdx) {
    return cameraListModes(ctx, deviceIdx);
  }

  public boolean start(int deviceIdx, int width, int height) {
    return cameraStart(ctx, deviceIdx, width, height);
  }

  public boolean stop() {
    return cameraStop(ctx);
  }

  /** Get next image. */
  public int[] getFrame() {
    return cameraGetFrame(ctx);
  }

  /** Mirror horizontally. */
  private int[] mirror(int[] px) {
    if (mirror == null || mirror.length != px.length) {
      mirror = new int[px.length];
    }
    int width = getWidth();
    int height = getHeight();
    int src = 0;
    int dst = 0;
    for(int y=0;y<height;y++) {
      src += width;
      for(int x=0;x<width;x++) {
        mirror[dst++] = px[--src];
      }
      src += width;
    }
    return mirror;
  }

  /** Get next image and mirror horizontally. */
  public int[] getFrameMirror() {
    int[] px = cameraGetFrame(ctx);
    if (px == null) return null;
    return mirror(px);
  }

  /** Flip image vertically. */
  private int[] flip(int[] px) {
    if (flip == null || flip.length != px.length) {
      flip = new int[px.length];
    }
    int width = getWidth();
    int height = getHeight();
    int src = 0;
    int dst = (height - 1) * width;
    for(int y=0;y<height;y++) {
      for(int x=0;x<width;x++) {
        flip[dst++] = px[src++];
      }
      dst -= width * 2;
    }
    return flip;
  }

  /** Get next image and flip vertically. */
  public int[] getFrameFlip() {
    int[] px = cameraGetFrame(ctx);
    if (px == null) return null;
    return flip(px);
  }

  /** Get next image and mirror horizontally and flip vertically. */
  public int[] getFrameMirrorAndFlip() {
    int[] px = cameraGetFrame(ctx);
    if (px == null) return null;
    px = mirror(px);
    px = flip(px);
    return px;
  }

  public int getWidth() {
    return cameraGetWidth(ctx);
  }

  public int getHeight() {
    return cameraGetHeight(ctx);
  }
}
