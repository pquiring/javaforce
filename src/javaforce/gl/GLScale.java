package javaforce.gl;

/** Stores scale. */

public class GLScale {
  public float x, y, z;
  public GLScale() {}
  public GLScale(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  public void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  public void add(float x, float y, float z) {
    this.x *= x;
    this.y *= y;
    this.z *= z;
  }
};

