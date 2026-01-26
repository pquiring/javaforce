package javaforce.media;

/** Camera native API
 *
 * @author pquiring
 */

import javaforce.ffm.*;
import javaforce.jni.*;

public interface CameraAPI {
  public static CameraAPI getInstance() {
    if (FFM.enabled()) {
      return CameraFFM.getInstance();
    } else {
      return CameraJNI.getInstance();
    }
  }

  public long cameraInit();
  public boolean cameraUninit(long ctx);
  public String[] cameraListDevices(long ctx);
  public String[] cameraListModes(long ctx, int deviceIdx);
  public boolean cameraStart(long ctx, int deviceIdx, int width, int height);
  public boolean cameraStop(long ctx);
  public int[] cameraGetFrame(long ctx);
  public int cameraGetWidth(long ctx);
  public int cameraGetHeight(long ctx);
}
