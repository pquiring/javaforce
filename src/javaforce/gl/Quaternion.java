/** Quaternion
 *
 * See : https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
 *
 */

package javaforce.gl;

public class Quaternion {
  public float x,y,z,w;

  /** Set values based on Euler Angles (x,y,z). */
  public void set(float x, float y, float z) {
    float cr = (float)Math.cos(x * 0.5);
    float sr = (float)Math.sin(x * 0.5);
    float cp = (float)Math.cos(y * 0.5);
    float sp = (float)Math.sin(y * 0.5);
    float cy = (float)Math.cos(z * 0.5);
    float sy = (float)Math.sin(z * 0.5);

    this.w = cr * cp * cy + sr * sp * sy;
    this.x = sr * cp * cy - cr * sp * sy;
    this.y = cr * sp * cy + sr * cp * sy;
    this.z = cr * cp * sy - sr * sp * cy;
  }

  /** Set values based on Euler Angles (Angles3). */
  public void set(Angles3 a3) {
    set(a3.v[0], a3.v[1], a3.v[2]);
  }

  public String toString() {
    return String.format("%.3f,%.3f,%.3f,%.3f\r\n", x, y, z, w);
  }
};
