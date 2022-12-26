package javaforce.gl;

/** Stores euler angles (x,y,z).
 * See https://en.wikipedia.org/wiki/Euler_angles
 */

public class Angles3 {
  public float v[] = new float[3];
  public Angles3() { }
  public Angles3(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
  }
  public void set(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
  }

  /** Create Euler Angles from Quaternion. */
  public static Angles3 toAngles3(Quaternion q) {
    Angles3 angles = new Angles3();

    // roll (x-axis rotation)
    float sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
    float cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
    angles.v[0] = (float)Math.atan2(sinr_cosp, cosr_cosp);

    // pitch (y-axis rotation)
    float sinp = (float)Math.sqrt(1 + 2 * (q.w * q.x - q.y * q.z));
    float cosp = (float)Math.sqrt(1 - 2 * (q.w * q.x - q.y * q.z));
    angles.v[1] = 2.0f * (float)Math.atan2(sinp, cosp) - (float)Math.PI / 2.0f;

    // yaw (z-axis rotation)
    float siny_cosp = 2 * (q.w * q.z + q.x * q.y);
    float cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
    angles.v[2] = (float)Math.atan2(siny_cosp, cosy_cosp);

    return angles;
  }

  public String toString() {
    return String.format("%.3f,%.3f,%.3f\r\n", v[0], v[1], v[2]);
  }

}
