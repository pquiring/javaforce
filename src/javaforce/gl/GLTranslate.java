package javaforce.gl;

/** Stores a translation (movement). */

public class GLTranslate {
  public float x, y, z;
  public GLTranslate() {}
  public GLTranslate(float x, float y, float z) {
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
    this.x += x;
    this.y += y;
    this.z += z;
  }
};

