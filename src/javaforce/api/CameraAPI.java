package javaforce.api;

import javaforce.ffm.*;

/** Camera native API.
 *
 * @author pquiring
 */

public interface CameraAPI {
  public static CameraAPI getInstance() {
    return CameraFFM.getInstance();
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
