package javaforce.gl;

/** Quaternion
 *
 * See : https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
 *
 */

import javaforce.*;

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

  /** Set values based on Euler Angles (x,y,z). */
  public void set(float x, float y, float z, Angles3.Order order) {
    float c1 = (float)Math.cos( x / 2 );
    float c2 = (float)Math.cos( y / 2 );
    float c3 = (float)Math.cos( z / 2 );

    float s1 = (float)Math.sin( x / 2 );
    float s2 = (float)Math.sin( y / 2 );
    float s3 = (float)Math.sin( z / 2 );

    switch ( order ) {
      case XYZ:
        this.x = s1 * c2 * c3 + c1 * s2 * s3;
        this.y = c1 * s2 * c3 - s1 * c2 * s3;
        this.z = c1 * c2 * s3 + s1 * s2 * c3;
        this.w = c1 * c2 * c3 - s1 * s2 * s3;
        break;
      case YXZ:
        this.x = s1 * c2 * c3 + c1 * s2 * s3;
        this.y = c1 * s2 * c3 - s1 * c2 * s3;
        this.z = c1 * c2 * s3 - s1 * s2 * c3;
        this.w = c1 * c2 * c3 + s1 * s2 * s3;
        break;
      case ZXY:
        this.x = s1 * c2 * c3 - c1 * s2 * s3;
        this.y = c1 * s2 * c3 + s1 * c2 * s3;
        this.z = c1 * c2 * s3 + s1 * s2 * c3;
        this.w = c1 * c2 * c3 - s1 * s2 * s3;
        break;
      case ZYX:
        this.x = s1 * c2 * c3 - c1 * s2 * s3;
        this.y = c1 * s2 * c3 + s1 * c2 * s3;
        this.z = c1 * c2 * s3 - s1 * s2 * c3;
        this.w = c1 * c2 * c3 + s1 * s2 * s3;
        break;
      case YZX:
        this.x = s1 * c2 * c3 + c1 * s2 * s3;
        this.y = c1 * s2 * c3 + s1 * c2 * s3;
        this.z = c1 * c2 * s3 - s1 * s2 * c3;
        this.w = c1 * c2 * c3 - s1 * s2 * s3;
        break;
      case XZY:
        this.x = s1 * c2 * c3 - c1 * s2 * s3;
        this.y = c1 * s2 * c3 - s1 * c2 * s3;
        this.z = c1 * c2 * s3 + s1 * s2 * c3;
        this.w = c1 * c2 * c3 + s1 * s2 * s3;
        break;
      default:
        JFLog.log("Error:Quaternion.set():Unknown order");
        break;
    }
  }

  /** Set values based on Euler Angles (Angles3). */
  public void set(Angles3 a3, Angles3.Order order) {
    set(a3.v[0], a3.v[1], a3.v[2], order);
  }

  public String toString() {
    return String.format("%.3f,%.3f,%.3f,%.3f\r\n", w, x, y, z);
  }
};
