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

  //web camera (no context - only one use per app)
  private native boolean cameraInit();
  private native boolean cameraUninit();
  private native String[] cameraListDevices();
  private native String[] cameraListModes(int deviceIdx);
  private native boolean cameraStart(int deviceIdx, int width, int height);
  private native boolean cameraStop();
  private native int[] cameraGetFrame();
  private native int cameraGetWidth();
  private native int cameraGetHeight();

  public boolean init() {
    return cameraInit();
  }

  public boolean uninit() {
    return cameraUninit();
  }

  public String[] listDevices() {
    return cameraListDevices();
  }

  public String[] listModes(int deviceIdx) {
    return cameraListModes(deviceIdx);
  }

  public boolean start(int deviceIdx, int width, int height) {
    return cameraStart(deviceIdx, width, height);
  }

  public boolean stop() {
    return cameraStop();
  }

  public int[] getFrame() {
    return cameraGetFrame();
  }

  public int[] getFrameMirror() {
    int[] px = cameraGetFrame();
    if (px == null) return null;
    //flip image horizontal (mirror)
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

  public int getWidth() {
    return cameraGetWidth();
  }

  public int getHeight() {
    return cameraGetHeight();
  }
}
