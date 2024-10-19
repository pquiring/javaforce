package javaforce.gl;

/**
 * Represents a plane in 3d space.
 *
 * @author pquiring
 */

public class Plane3 {
  private Vector3 aux1 = new Vector3();
  private Vector3 aux2 = new Vector3();

  public Vector3 normal = new Vector3();
  public float d;

  public void set3Points(Vector3 v1, Vector3 v2, Vector3 v3) {
    aux1.sub(v1, v2);
    aux2.sub(v3, v2);

    normal.cross(aux2, aux1);
    normal.normalize();
    d = -normal.dot(v2);
  }

  public void setNormalAndPoint(Vector3 normal, Vector3 point) {
    this.normal.set(normal);
    this.normal.normalize();
    d = -(this.normal.dot(point));
  }

  public void setCoefficients(float a, float b, float c, float d) {
    // set the normal vector
    normal.set(a,b,c);
    //compute the lenght of the vector
    float l = normal.length();
    // normalize the vector
    normal.set(a/l,b/l,c/l);
    // and divide d by th length as well
    this.d = d/l;
  }

  public float distance(Vector3 p) {
    return (d + normal.dot(p));
  }

  public void print() {
    System.out.println(String.format("Plane3:%7.3f,%7.3f,%7.3f : %7.3f", normal.v[0], normal.v[1], normal.v[2], d));
  }
}
