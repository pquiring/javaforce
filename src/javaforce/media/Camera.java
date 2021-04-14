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

  //web camera (no context - only one use per app)
  private native boolean cameraInit();
  private native boolean cameraUninit();
  private native String[] cameraListDevices();
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

  public boolean start(int deviceIdx, int width, int height) {
    return cameraStart(deviceIdx, width, height);
  }

  public boolean stop() {
    return cameraStop();
  }

  public int[] getFrame() {
    return cameraGetFrame();
  }

  public int getWidth() {
    return cameraGetWidth();
  }

  public int getHeight() {
    return cameraGetHeight();
  }
}
