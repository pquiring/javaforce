package javaforce.gl;

/**
 * Stores Euler angles (x,y,z). See https://en.wikipedia.org/wiki/Euler_angles
 */
import javaforce.*;

public class Angles3 {

  public enum Order {
    XYZ, YXZ, ZXY, ZYX, YZX, XZY
  };
  public float[] v = new float[3];

  public Angles3() {
  }

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

  private float clamp(float value, float min, float max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  /**
   * Set angles based on Quaternion. Based on logic from three.js.
   */
  public void set(Quaternion q, Order order) {
    Matrix m = new Matrix();
    m.set(q);
    float[] te = m.m;
    float m11 = te[0], m12 = te[4], m13 = te[8];
    float m21 = te[1], m22 = te[5], m23 = te[9];
    float m31 = te[2], m32 = te[6], m33 = te[10];
    switch (order) {
      case XYZ:
        v[1] = (float) Math.asin(clamp(m13, - 1, 1));
        if ((float) Math.abs(m13) < 0.9999999) {
          v[0] = (float) Math.atan2(-m23, m33);
          v[2] = (float) Math.atan2(-m12, m11);
        } else {
          v[0] = (float) Math.atan2(m32, m22);
          v[2] = 0;

        }
        break;
      case YXZ:
        v[0] = (float) Math.asin(-clamp(m23, - 1, 1));
        if ((float) Math.abs(m23) < 0.9999999) {
          v[1] = (float) Math.atan2(m13, m33);
          v[2] = (float) Math.atan2(m21, m22);
        } else {
          v[1] = (float) Math.atan2(-m31, m11);
          v[2] = 0;
        }
        break;
      case ZXY:
        v[0] = (float) Math.asin(clamp(m32, - 1, 1));
        if ((float) Math.abs(m32) < 0.9999999) {
          v[1] = (float) Math.atan2(-m31, m33);
          v[2] = (float) Math.atan2(-m12, m22);
        } else {
          v[1] = 0;
          v[2] = (float) Math.atan2(m21, m11);
        }
        break;
      case ZYX:
        v[1] = (float) Math.asin(-clamp(m31, - 1, 1));
        if ((float) Math.abs(m31) < 0.9999999) {
          v[0] = (float) Math.atan2(m32, m33);
          v[2] = (float) Math.atan2(m21, m11);
        } else {
          v[0] = 0;
          v[2] = (float) Math.atan2(-m12, m22);
        }
        break;
      case YZX:
        v[2] = (float) Math.asin(clamp(m21, - 1, 1));
        if ((float) Math.abs(m21) < 0.9999999) {
          v[0] = (float) Math.atan2(-m23, m22);
          v[1] = (float) Math.atan2(-m31, m11);
        } else {
          v[0] = 0;
          v[1] = (float) Math.atan2(m13, m33);
        }
        break;
      case XZY:
        v[2] = (float) Math.asin(-clamp(m12, - 1, 1));
        if ((float) Math.abs(m12) < 0.9999999) {
          v[0] = (float) Math.atan2(m32, m22);
          v[1] = (float) Math.atan2(m13, m11);
        } else {
          v[0] = (float) Math.atan2(-m23, m33);
          v[1] = 0;
        }
        break;
      default:
        JFLog.log("Error:Angles3.set(Quaternion,Order):Unknown order");
        break;
    }
  }

  public String toString() {
    return String.format("%.3f,%.3f,%.3f\r\n", v[0], v[1], v[2]);
  }
}
