package javaforce.gl;

/** Stores one vector. */

public class GLVector4 {
  public float v[] = new float[4];
  public GLVector4() { }
  public GLVector4(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
    this.v[3] = 1.0f;
  }
  public GLVector4(float x, float y, float z, float w) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
    this.v[3] = w;
  }
  public void set(float x, float y, float z) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
  }
  public void set(float x, float y, float z,float w) {
    this.v[0] = x;
    this.v[1] = y;
    this.v[2] = z;
    this.v[3] = w;
  }
  /** this = a - b */
  public void sub(GLVector4 a, GLVector4 b) {
    v[0] = a.v[0] - b.v[0];
    v[1] = a.v[1] - b.v[1];
    v[2] = a.v[2] - b.v[2];
  }
  /** this = a X b */
  public void cross(GLVector4 a, GLVector4 b) {
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
  public float dot(GLVector4 in) {
    return v[0] * in.v[0] + v[1] * in.v[1] + v[2] * in.v[2];
  }
  //length relative to another vertex
  public float length(GLVector4 in) {
    float _x = v[0] - in.v[0];
    float _y = v[1] - in.v[1];
    float _z = v[2] - in.v[2];
    return (float) Math.sqrt(_x * _x + _y * _y + _z * _z);
  }
  public String toString() {
    return String.format("%.3f,%.3f,%.3f,%.3f\r\n", v[0], v[1], v[2], v[3]);
  }
};
