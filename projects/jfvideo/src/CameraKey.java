/**
 * Camera Key Point
 *
 * @author pquiring
 *
 * Created : Sept 7, 2013
 */

public class CameraKey {
  public int offset;  //in seconds
  public float tx, ty, tz;  //translate
  public float rx, ry, rz;  //rotate
  public float fov;
  public CameraKey() {
    fov = 60.0f;
  }
}
