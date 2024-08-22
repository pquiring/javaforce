package service;

/** CameraWorker interface.
 *
 * @author pquiring
 */

public interface CameraWorker {
  public void start();
  public void cancel();
  public void reloadConfig();
  public Camera getCamera();
  public int getLog();
}
