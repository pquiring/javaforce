package javaforce.gl;

/** Store one vertex (point). */

public class Vertex {
  public float x,y,z;
  public float u,v;  //texture coords
  public Vertex() { }
  public Vertex(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  public Vertex(float x, float y, float z, float u, float v) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.u = u;
    this.v = v;
  }
  public Vertex set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
    return this;
  }
  public Vertex set(float x, float y, float z, float u, float v) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.u = u;
    this.v = v;
    return this;
  }
  /** this = a - b */
  public void sub(Vertex a, Vertex b) {
    x = a.x - b.x;
    y = a.y - b.y;
    z = a.z - b.z;
  }
  /** this = a X b */
  public void cross(Vertex a, Vertex b) {
    x = a.y * b.z - a.z * b.y;
    y = a.z * b.x - a.x * b.z;
    z = a.x * b.y - a.y * b.x;
  }
  /** normalize this vertex */
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
    x *= s;
    y *= s;
    z *= s;
  }
  public float dot(Vertex v) {
    return x * v.x + y * v.y + z * v.z;
  }
  //length relative to another vertex
  public float length(Vertex v) {
    float _x = x - v.x;
    float _y = y - v.y;
    float _z = z - v.z;
    return (float) Math.sqrt(_x * _x + _y * _y + _z * _z);
  }
};

