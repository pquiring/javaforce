package javaforce.gl;

/** Stores one vector (x,y,z). */

public class Vector3 {
  public float v[] = new float[3];
  public Vector3() { }
  public Vector3(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
  }
  public void set(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
  }
  public void set(Vector3 in) {
    this.v[0] = in.v[0];
    this.v[1] = in.v[1];
    this.v[2] = in.v[2];
  }
  public void set(Vector4 in) {
    this.v[0] = in.v[0];
    this.v[1] = in.v[1];
    this.v[2] = in.v[2];
  }
  /** this = a + b */
  public void add(Vector3 a, Vector3 b) {
    v[0] = a.v[0] + b.v[0];
    v[1] = a.v[1] + b.v[1];
    v[2] = a.v[2] + b.v[2];
  }
  /** this += a */
  public void add(Vector3 a) {
    v[0] += a.v[0];
    v[1] += a.v[1];
    v[2] += a.v[2];
  }
  /** this = a - b */
  public void sub(Vector3 a, Vector3 b) {
    v[0] = a.v[0] - b.v[0];
    v[1] = a.v[1] - b.v[1];
    v[2] = a.v[2] - b.v[2];
  }
  /** this -= a */
  public void sub(Vector3 a) {
    v[0] -= a.v[0];
    v[1] -= a.v[1];
    v[2] -= a.v[2];
  }
  /** this = a X b */
  public void cross(Vector3 a, Vector3 b) {
    v[0] = a.v[1] * b.v[2] - a.v[2] * b.v[1];
    v[1] = a.v[2] * b.v[0] - a.v[0] * b.v[2];
    v[2] = a.v[0] * b.v[1] - a.v[1] * b.v[0];
  }
  /** normalize this vector */
  public void normalize() {
    float len = length();
    if (len == 0.0f) return;
    scale(1.0f / len);
  }
  public float length() {
    return (float) Math.sqrt(lengthSquared());
  }
  public float lengthSquared() {
    return dot(this);
  }
  public void scale(float s) {
    v[0] *= s;
    v[1] *= s;
    v[2] *= s;
  }
  public void divide(float d) {
    if (d == 0f) return;
    v[0] /= d;
    v[1] /= d;
    v[2] /= d;
  }
  public float dot(Vector3 in) {
    return v[0] * in.v[0] + v[1] * in.v[1] + v[2] * in.v[2];
  }
  //length relative to another vertex
  public float length(Vector3 in) {
    float _x = v[0] - in.v[0];
    float _y = v[1] - in.v[1];
    float _z = v[2] - in.v[2];
    return (float) Math.sqrt(_x * _x + _y * _y + _z * _z);
  }

  public static Vector3 toVector3(Quaternion q) {
    Vector3 angles = new Vector3();

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
};
