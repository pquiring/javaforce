package javaforce.gl;

/** Stores angle-axis rotation. */

public class GLRotate {  //rotate
  public float angle, x, y, z;
  public GLRotate() {}
  public GLRotate(float angle, float x, float y, float z) {
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

