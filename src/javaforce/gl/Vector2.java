package javaforce.gl;

/** Vector2 stores one vector (x,y). */

public class Vector2 {
  public float[] v = new float[2];
  public Vector2() { }
  public Vector2(float x, float y) {
    this.v[0] = x;
    this.v[1] = y;
  }
  public Vector2(float[] xy) {
    this.v[0] = xy[0];
    this.v[1] = xy[1];
  }
  public void set(float x, float y) {
    this.v[0] = x;
    this.v[1] = y;
  }
  public void set(Vector2 in) {
    this.v[0] = in.v[0];
    this.v[1] = in.v[1];
  }
  public void set(Vector4 in) {
    this.v[0] = in.v[0];
    this.v[1] = in.v[1];
  }
  /** this = a + b */
  public void add(Vector2 a, Vector2 b) {
    v[0] = a.v[0] + b.v[0];
    v[1] = a.v[1] + b.v[1];
  }
  /** this += a */
  public void add(Vector2 a) {
    v[0] += a.v[0];
    v[1] += a.v[1];
  }
  /** this = a - b */
  public void sub(Vector2 a, Vector2 b) {
    v[0] = a.v[0] - b.v[0];
    v[1] = a.v[1] - b.v[1];
  }
  /** this -= a */
  public void sub(Vector2 a) {
    v[0] -= a.v[0];
    v[1] -= a.v[1];
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
  }
  public void divide(float d) {
    if (d == 0f) return;
    v[0] /= d;
    v[1] /= d;
  }
  public float dot(Vector2 in) {
    return v[0] * in.v[0] + v[1] * in.v[1];
  }
  //length relative to another vertex
  public float length(Vector2 in) {
    float _x = v[0] - in.v[0];
    float _y = v[1] - in.v[1];
    return (float) Math.sqrt(_x * _x + _y * _y);
  }

  public String toString() {
    return String.format("%.3f,%.3f", v[0], v[1]);
  }
};
