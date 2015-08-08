package javaforce.gl;

/**
 * Represents the frustum (viewable area of a perspective)
 *
 * @author pquiring
 */

public class GLFrustum {
  private GLVector3 ntl, ntr, nbl, nbr, ftl, ftr, fbl, fbr;
  private float nearD, farD;
  private float nw, nh, fw, fh;
  private GLPlane pl[];
  private GLVector3 nc, fc, X, Y, Z;
  private GLVector3 XX, YY, ZZ;

  private GLVector3 _p, _l, _u;  //pointers

  public GLFrustum() {
    ntl = new GLVector3();
    ntr = new GLVector3();
    nbl = new GLVector3();
    nbr = new GLVector3();
    ftl = new GLVector3();
    ftr = new GLVector3();
    fbl = new GLVector3();
    fbr = new GLVector3();
    nc = new GLVector3();
    fc = new GLVector3();
    X = new GLVector3();
    Y = new GLVector3();
    Z = new GLVector3();
    XX = new GLVector3();
    YY = new GLVector3();
    ZZ = new GLVector3();
    pl = new GLPlane[6];
    for (int a = 0; a < 6; a++) {
      pl[a] = new GLPlane();
    }
  }

  private static final float ANG2RAD = (float) Math.PI / 180f;

  public void setPerspecive(float angle, float ratio, float near, float far) {
    nearD = near;
    farD = far;

    float tan = (float) Math.tan(angle * ANG2RAD * 0.5);
    nh = nearD * tan;
    nw = nh * ratio;
    fh = farD * tan;
    fw = fh * ratio;
  }

  private static final int TOP = 0;
  private static final int BOTTOM = 1;
  private static final int LEFT = 2;
  private static final int RIGHT = 3;
  private static final int NEARP = 4;
  private static final int FARP = 5;

  public void setPosition(GLVector3 p, GLVector3 l, GLVector3 u) {
    _p = p;
    _l = l;
    _u = u;
    Z.sub(p, l);
    Z.normalize();

    X.cross(u, Z);
    X.normalize();

    Y.cross(Z, X);

    ZZ.set(Z);
    ZZ.scale(nearD);
    nc.sub(p, ZZ);
    ZZ.set(Z);
    ZZ.scale(farD);
    fc.sub(p, ZZ);

//    ntl = nc + Y * nh - X * nw;
//    ntr = nc + Y * nh + X * nw;
//    nbl = nc - Y * nh - X * nw;
//    nbr = nc - Y * nh + X * nw;
    YY.set(Y);
    YY.scale(nh);
    XX.set(X);
    XX.scale(nw);

    ntl.add(nc, YY);
    ntl.sub(XX);
    ntr.add(nc, YY);
    ntr.add(XX);
    nbl.sub(nc, YY);
    nbl.sub(XX);
    nbr.sub(nc, YY);
    nbr.add(XX);

//    ftl = fc + Y * fh - X * fw;
//    ftr = fc + Y * fh + X * fw;
//    fbl = fc - Y * fh - X * fw;
//    fbr = fc - Y * fh + X * fw;
    YY.set(Y);
    YY.scale(fh);
    XX.set(X);
    XX.scale(fw);

    ftl.add(fc, YY);
    ftl.sub(XX);
    ftr.add(fc, YY);
    ftr.add(XX);
    fbl.sub(fc, YY);
    fbl.sub(XX);
    fbr.sub(fc, YY);
    fbr.add(XX);

    pl[TOP].set3Points(ntr, ntl, ftl);
    pl[BOTTOM].set3Points(nbl, nbr, fbr);
    pl[LEFT].set3Points(ntl, nbl, fbl);
    pl[RIGHT].set3Points(nbr, ntr, fbr);
    pl[NEARP].set3Points(ntl, ntr, nbr);
    pl[FARP].set3Points(ftr, ftl, fbl);
  }

	public static final int OUTSIDE = 0;
  public static final int INTERSECT = 1;
  public static final int INSIDE = 2;

  public int pointInside(GLVector3 p) {
    int result = INSIDE;
    float d;
    for(int i=0; i < 6; i++) {
      d = pl[i].distance(p);
      if (d < 0) return OUTSIDE;
      if (d == 0f) result = INTERSECT;
    }
    return result;
  }

  /** Tests if sphere is within frustum.
   * @param p = center if sphere
   * @param size = size of sphere (radius or diameter?)
   */
  public int sphereInside(GLVector3 p, float size) {
    float distance;
    int result = INSIDE;

    for(int i=0; i < 6; i++) {
      distance = pl[i].distance(p);
      if (distance < -size) return OUTSIDE;
      if (distance < size) result = INTERSECT;
    }
    return result;
  }

  /** Tests if box is within frustum.
   * @param pts = 8 points of box
   */
  public int boxInside(GLVector3 pts[]) {
    int result = INSIDE, in, out;
    for(int i=0; i < 6; i++) {
      // reset counters for corners in and out
      out=0;in=0;
      // for each corner of the box do ...
      // get out of the cycle as soon as a box as corners
      // both inside and out of the frustum
      for (int k = 0; k < 8 && (in==0 || out==0); k++) {
        // is the corner outside or inside
        if (pl[i].distance(pts[k]) < 0)
          out++;
        else
          in++;
      }
      //if all corners are out
      if (in == 0) return (OUTSIDE);
      // if some corners are out and others are in
      if (out > 0) result = INTERSECT;
    }
    return result;
  }

  public void print() {
    System.out.println(String.format("GLFrustum:%7.3f,%7.3f,%7.3f,%7.3f", nw, nh, fw, fh));
    System.out.println(String.format("GLFrustum:p:%7.3f,%7.3f,%7.3f", _p.v[0], _p.v[1], _p.v[2]));
    System.out.println(String.format("GLFrustum:l:%7.3f,%7.3f,%7.3f", _l.v[0], _l.v[1], _l.v[2]));
    System.out.println(String.format("GLFrustum:u:%7.3f,%7.3f,%7.3f", _u.v[0], _u.v[1], _u.v[2]));
    for(int a=0;a<6;a++) {
      pl[a].print();
    }
  }
}
