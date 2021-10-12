package javaforce.gl;

/** Stores a translation (movement). */

public class Translate3 {
  public float x, y, z;
  public Translate3() {}
  public Translate3(float x, float y, float z) {
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

