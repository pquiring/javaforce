package javaforce.gl;

/** Stores scale. */

public class Scale3 {
  public float x, y, z;
  public Scale3() {}
  public Scale3(float x, float y, float z) {
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

