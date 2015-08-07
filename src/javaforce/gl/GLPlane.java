package javaforce.gl;

/**
 * Represents a plane in 3d space.
 *
 * @author pquiring
 */

public class GLPlane {
  private GLVector3 aux1 = new GLVector3();
  private GLVector3 aux2 = new GLVector3();

  public GLVector3 normal = new GLVector3();
  public GLVector3 point = new GLVector3();
  public float d;

  public void set3Points(GLVector3 v1, GLVector3 v2, GLVector3 v3) {
    aux1.sub(v1, v2);
    aux2.sub(v3, v2);

    normal.cross(aux2, aux1);
    normal.normalize();
    point.set(v2);
    d = -normal.dot(point);
  }

  public void setNormalAndPoint(GLVector3 normal, GLVector3 point) {
    this.normal.set(normal);
    this.normal.normalize();
    this.point.set(point);  //this was missing in sample source
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

  public float distance(GLVector3 p) {
    return (d + normal.dot(p));
  }
}
