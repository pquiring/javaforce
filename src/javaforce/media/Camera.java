package javaforce.media;

import javaforce.JF;

import javaforce.jni.JFNative;
import javaforce.jni.LnxNative;
import javaforce.jni.MacNative;
import javaforce.jni.WinNative;

/**
 * Web Camera API
 *
 * @author pquiring
 *
 * Created : Aug 20, 2013
 */

public class Camera {
  private long ctx = 0;
  static {
    JFNative.load();
    if (JF.isWindows()) {
      WinNative.load();
    } else if (JF.isMac()) {
      MacNative.load();
    } else {
      LnxNative.load();
    }
  }

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
    if (!JFNative.loaded) return false;
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
