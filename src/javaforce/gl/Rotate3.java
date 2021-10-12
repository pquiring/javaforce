package javaforce.gl;

/** Stores angle-axis rotation. */

public class Rotate3 {  //rotate
  public float angle, x, y, z;
  public Rotate3() {}
  public Rotate3(float angle, float x, float y, float z) {
    this.angle = angle;
    this.x = x;
    this.y = y;
    this.z = z;
  }
  public void set(float angle, float x, float y, float z) {
    this.angle = angle;
    this.x = x;
    this.y = y;
    this.z = z;
  }
  public void add(float angle, float x, float y, float z) {
    //how???
  }
};
