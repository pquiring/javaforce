package javaforce.jni;

/** Camera JNI
 *
 * @author pquiring
 */

import javaforce.ffm.*;
import javaforce.api.*;

public class CameraJNI implements CameraAPI {
  private static CameraJNI instance;

  public static synchronized CameraJNI getInstance(FFMArray array) {
    if (instance == null) {
      JFNative.load();
      instance = new CameraJNI();
    }
    FFM.setFFMArray(array);
    return instance;
  }

  public native long cameraInit();
  public native boolean cameraUninit(long ctx);
  public native String[] cameraListDevices(long ctx);
  public native String[] cameraListModes(long ctx, int deviceIdx);
  public native boolean cameraStart(long ctx, int deviceIdx, int width, int height);
  public native boolean cameraStop(long ctx);
  public native int[] cameraGetFrame(long ctx);
  public native int cameraGetWidth(long ctx);
  public native int cameraGetHeight(long ctx);
}
