/** Quaternion
 *
 * See : https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
 *
 */

package javaforce.gl;

class Quaternion {
  public float x,y,z,w;

  public static Quaternion toQuaternion(float roll, float pitch, float yaw) {
    float cr = (float)Math.cos(roll * 0.5);
    float sr = (float)Math.sin(roll * 0.5);
    float cp = (float)Math.cos(pitch * 0.5);
    float sp = (float)Math.sin(pitch * 0.5);
    float cy = (float)Math.cos(yaw * 0.5);
    float sy = (float)Math.sin(yaw * 0.5);

    Quaternion q = new Quaternion();
    q.w = cr * cp * cy + sr * sp * sy;
    q.x = sr * cp * cy - cr * sp * sy;
    q.y = cr * sp * cy + sr * cp * sy;
    q.z = cr * cp * sy - sr * sp * cy;

    return q;
  }

  public static Quaternion toQuaternion(Vector3 a3) {
    return toQuaternion(a3.v[0], a3.v[1], a3.v[2]);
  }
};
