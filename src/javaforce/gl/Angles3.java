package javaforce.gl;

/** Stores euler angles (x,y,z).
 * See https://en.wikipedia.org/wiki/Euler_angles
 */

import javaforce.*;

public class Angles3 {
  public enum Order {XYZ, YXZ, ZXY, ZYX, YZX, XZY};
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

  /** Set angles based on Quaternion.
   * Based on logic from Wikipedia.
   */
  public void set(Quaternion q) {
    // roll (x-axis rotation)
    float sinr_cosp = 2.0f * (q.w * q.x + q.y * q.z);
    float cosr_cosp = 1.0f - 2.0f * (q.x * q.x + q.y * q.y);
    v[0] = (float)(float)Math.atan2(sinr_cosp, cosr_cosp);

    // pitch (y-axis rotation)
    float sinp = (float)(float)Math.sqrt(1.0f + 2.0f * (q.w * q.x - q.y * q.z));
    float cosp = (float)(float)Math.sqrt(1.0f - 2.0f * (q.w * q.x - q.y * q.z));
    v[1] = 2.0f * (float)(float)Math.atan2(sinp, cosp) - (float)(float)Math.PI / 2.0f;

    // yaw (z-axis rotation)
    float siny_cosp = 2.0f * (q.w * q.z + q.x * q.y);
    float cosy_cosp = 1.0f - 2.0f * (q.y * q.y + q.z * q.z);
    v[2] = (float)(float)Math.atan2(siny_cosp, cosy_cosp);
  }

  private float clamp(float value, float min, float max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  /** Set angles based on Quaternion.
   * Based on logic from three.js.
   */
  public void set(Quaternion q, Order order) {
    Matrix m = new Matrix();
    m.set(q);
    float[] te = m.m;
		float m11 = te[ 0 ], m12 = te[ 4 ], m13 = te[ 8 ];
		float m21 = te[ 1 ], m22 = te[ 5 ], m23 = te[ 9 ];
		float m31 = te[ 2 ], m32 = te[ 6 ], m33 = te[ 10 ];
    switch ( order ) {
			case XYZ:
				v[1] = (float)Math.asin( clamp( m13, - 1, 1 ) );
				if ( (float)Math.abs( m13 ) < 0.9999999 ) {
					v[0] = (float)Math.atan2( - m23, m33 );
					v[2] = (float)Math.atan2( - m12, m11 );
				} else {
					v[0] = (float)Math.atan2( m32, m22 );
  				v[2] = 0;

				}
				break;
			case YXZ:
				v[0] = (float)Math.asin( - clamp( m23, - 1, 1 ) );
				if ( (float)Math.abs( m23 ) < 0.9999999 ) {
					v[1] = (float)Math.atan2( m13, m33 );
  				v[2] = (float)Math.atan2( m21, m22 );
				} else {
					v[1] = (float)Math.atan2( - m31, m11 );
					v[2] = 0;
				}
				break;
			case ZXY:
				v[0] = (float)Math.asin( clamp( m32, - 1, 1 ) );
				if ( (float)Math.abs( m32 ) < 0.9999999 ) {
					v[1] = (float)Math.atan2( - m31, m33 );
					v[2] = (float)Math.atan2( - m12, m22 );
				} else {
					v[1] = 0;
					v[2] = (float)Math.atan2( m21, m11 );
				}
				break;
			case ZYX:
				v[1] = (float)Math.asin( - clamp( m31, - 1, 1 ) );
				if ( (float)Math.abs( m31 ) < 0.9999999 ) {
					v[0] = (float)Math.atan2( m32, m33 );
					v[2] = (float)Math.atan2( m21, m11 );
				} else {
					v[0] = 0;
					v[2] = (float)Math.atan2( - m12, m22 );
				}
				break;
			case YZX:
				v[2] = (float)Math.asin( clamp( m21, - 1, 1 ) );
				if ( (float)Math.abs( m21 ) < 0.9999999 ) {
					v[0] = (float)Math.atan2( - m23, m22 );
					v[1] = (float)Math.atan2( - m31, m11 );
				} else {
					v[0] = 0;
					v[1] = (float)Math.atan2( m13, m33 );
				}
				break;
			case XZY:
				v[2] = (float)Math.asin( - clamp( m12, - 1, 1 ) );
				if ( (float)Math.abs( m12 ) < 0.9999999 ) {
					v[0] = (float)Math.atan2( m32, m22 );
					v[1] = (float)Math.atan2( m13, m11 );
				} else {
					v[0] = (float)Math.atan2( - m23, m33 );
					v[1] = 0;
				}
				break;
			default:
        JFLog.log("Error:Angles3.set(Quaternion,Order):Unknown order");
    }
  }

  public String toString() {
    return String.format("%.3f,%.3f,%.3f\r\n", v[0], v[1], v[2]);
  }
}
