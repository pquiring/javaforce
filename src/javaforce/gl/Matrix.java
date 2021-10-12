package javaforce.gl;

/** 4x4 matrix */
public class Matrix implements Cloneable {
  public float m[] = new float[16];
  private float r[];  //result temp

  private Matrix mat;  //only need m[], setAA(), setTranslate(), setScale(), set(), get()
  private Vector3 vec;

  public Matrix() {
    setIdentity();
    mat = new Matrix(false);  //do not need mat,vec,r in mat
    vec = new Vector3();
    r = new float[16];
  }
  //this ctor does not create mat,vec,r
  private Matrix(boolean dummy) {
    setIdentity();
  }
  public Object clone() {
    Matrix cln = new Matrix();
    for(int a=0;a<16;a++) cln.m[a] = m[a];
    return cln;
  }
  public void setIdentity() {
    for(int a=0;a<16;a++) {
      if (a % 5 == 0)
        m[a] = 1.0f;
      else
        m[a] = 0.0f;
    }
  }
  public void setIdentity3x3() {  //effectively reset rotation
    for(int a=0;a<11;a++) {
      if (a % 5 == 0)
        m[a] = 1.0f;
      else
        m[a] = 0.0f;
    }
  }
  public void set(Matrix src) {
    for(int a=0;a<16;a++) m[a] = src.m[a];
  }
  //convert angle-axis(vector) into matrix (en.wikipedia.org/wiki/Axis_angle)
  public void setAA(float angle, float x, float y, float z) {
    float xx, yy, zz, xy, yz, zx, xs, ys, zs, one_c, s, c;

    s = (float)Math.sin( angle * (float)Math.PI / 180.0f );
    c = (float)Math.cos( angle * (float)Math.PI / 180.0f );

    setIdentity();

    if (x == 0.0f) {
      if (y == 0.0f) {
        if (z != 0.0f) {
          /* rotate only around z-axis */
          m[0+0*4] = c;
          m[1+1*4] = c;
          if (z < 0.0) {
            m[0+1*4] = s;
            m[1+0*4] = -s;
          } else {
            m[0+1*4] = -s;
            m[1+0*4] = s;
          }
          return;
        }
      } else if (z == 0.0f) {
        /* rotate only around y-axis */
        m[0+0*4] = c;
        m[2+2*4] = c;
        if (y < 0.0) {
          m[0+2*4] = -s;
          m[2+0*4] = s;
        } else {
          m[0+2*4] = s;
          m[2+0*4] = -s;
        }
        return;
      }
    } else if (y == 0.0f) {
      if (z == 0.0f) {
        /* rotate only around x-axis */
        m[1+1*4] = c;
        m[2+2*4] = c;
        if (x < 0.0) {
          m[1+2*4] = s;
          m[2+1*4] = -s;
        } else {
          m[1+2*4] = -s;
          m[2+1*4] = s;
        }
        return;
      }
    }

    float mag;
    //complex rotation
    mag = (float)Math.sqrt(x * x + y * y + z * z);
    if (mag <= 1.0e-4f) return;    //rotation too small
    x /= mag;
    y /= mag;
    z /= mag;
    xx = x * x;
    yy = y * y;
    zz = z * z;
    xy = x * y;
    yz = y * z;
    zx = z * x;
    xs = x * s;
    ys = y * s;
    zs = z * s;
    one_c = 1.0f - c;
    m[0+0*4] = (one_c * xx) + c;
    m[0+1*4] = (one_c * xy) - zs;
    m[0+2*4] = (one_c * zx) + ys;
    m[1+0*4] = (one_c * xy) + zs;
    m[1+1*4] = (one_c * yy) + c;
    m[1+2*4] = (one_c * yz) - xs;
    m[2+0*4] = (one_c * zx) - ys;
    m[2+1*4] = (one_c * yz) + xs;
    m[2+2*4] = (one_c * zz) + c;
  }

  public void setAATranslate(float angle, float x, float y, float z, float tx, float ty, float tz) {
    setAA(angle,x,y,z);  //sets identity
    addTranslate(tx, ty, tz);
  }

  public void setTranslate(float x, float y, float z) {
    setIdentity();
    m[0+3*4] = x;
    m[1+3*4] = y;
    m[2+3*4] = z;
  }

  public void setScale(float x, float y, float z) {
    setIdentity();
    m[0+0*4] = x;
    m[1+1*4] = y;
    m[2+2*4] = z;
  }

  /** Adds rotation assuming there is currently no translation. */
  public void addRotate(float angle, float ax, float ay, float az) {
    mat.setAA(angle, ax, ay, az);
    mult3x3(mat);
  }

  /** Adds rotation with current translation. */
  public void addRotate2(float angle, float ax, float ay, float az) {
    mat.setAA(angle, ax, ay, az);
    mult4x4(mat);
  }

  /** Adds rotation adjusted to current rotation but assuming there is currently no translation. */
  public void addRotate3(float angle, float ax, float ay, float az) {
    vec.v[0] = ax;
    vec.v[1] = ay;
    vec.v[2] = az;
    mult(vec);
    mat.setAA(angle, vec.v[0], vec.v[1], vec.v[2]);
    mult3x3(mat);
  }

  /** Adds rotation adjusted to current rotation with current translation. */
  public void addRotate4(float angle, float ax, float ay, float az) {
    vec.v[0] = ax;
    vec.v[1] = ay;
    vec.v[2] = az;
    mult(vec);
    mat.setAA(angle, vec.v[0], vec.v[1], vec.v[2]);
    mult4x4(mat);
  }

  /** Adds translation assuming there is currently no rotation. */
  public void addTranslate(float tx, float ty, float tz) {
    m[0+3*4] += tx;
    m[1+3*4] += ty;
    m[2+3*4] += tz;
  }

  /** Adds translation assuming there is currently no rotation. */
  public void addTranslate(Matrix src) {
    addTranslate(src.m[12], src.m[13], src.m[14]);
  }

  /** Adds translation with current rotation. */
  public void addTranslate2(float tx, float ty, float tz) {
    mat.setTranslate(tx, ty, tz);
    mult4x4(mat);
  }

  /** Adds scale using full matrix multiple. */
  public void addScale(float sx, float sy, float sz) {
    mat.setScale(sx, sy, sz);
    mult4x4(mat);
  }

  /** Multiply this matrix with another */
  public void mult4x4(Matrix src) {
    //64 mult
    float a0, a1, a2, a3;
    for(int col=0;col<4;col++) {
      a0 = m[col+0*4];
      a1 = m[col+1*4];
      a2 = m[col+2*4];
      a3 = m[col+3*4];
      r[col+0*4] = a0 * src.m[0+0*4] + a1 * src.m[1+0*4] + a2 * src.m[2+0*4] + a3 * src.m[3+0*4];
      r[col+1*4] = a0 * src.m[0+1*4] + a1 * src.m[1+1*4] + a2 * src.m[2+1*4] + a3 * src.m[3+1*4];
      r[col+2*4] = a0 * src.m[0+2*4] + a1 * src.m[1+2*4] + a2 * src.m[2+2*4] + a3 * src.m[3+2*4];
      r[col+3*4] = a0 * src.m[0+3*4] + a1 * src.m[1+3*4] + a2 * src.m[2+3*4] + a3 * src.m[3+3*4];
    }
    //swap m/r
    float[] x;
    x = m;
    m = r;
    r = x;
  }

  /** Multiply this matrix with another (rotation/scale only) */
  public void mult3x3(Matrix src) {
    //27 mult
    float a0, a1, a2;
    for(int i=0;i<3;i++) {
      a0 = m[i+0*4];
      a1 = m[i+1*4];
      a2 = m[i+2*4];
      r[i+0*4] = a0 * src.m[0+0*4] + a1 * src.m[1+0*4] + a2 * src.m[2+0*4];
      r[i+1*4] = a0 * src.m[0+1*4] + a1 * src.m[1+1*4] + a2 * src.m[2+1*4];
      r[i+2*4] = a0 * src.m[0+2*4] + a1 * src.m[1+2*4] + a2 * src.m[2+2*4];
    }
    for(int a=0;a<4;a++) r[a+3*4] = m[a+3*4];
    //swap m/r
    float[] x;
    x = m;
    m = r;
    r = x;
  }

  /** 3x3 matrix multiple (rotation only) */
  public void mult(Matrix src) {
    mult3x3(src);
  }

  /** Multiply another 3x1 matrix with this one (3x3 part only)
   *  Effectively rotates the GLVector3 by the rotation of this matrix
   */
  public void mult(Vector3 dest) {
    float nx, ny, nz;
    nx = m[0+0*4] * dest.v[0] + m[1+0*4] * dest.v[1] + m[2+0*4] * dest.v[2];
    ny = m[0+1*4] * dest.v[0] + m[1+1*4] * dest.v[1] + m[2+1*4] * dest.v[2];
    nz = m[0+2*4] * dest.v[0] + m[1+2*4] * dest.v[1] + m[2+2*4] * dest.v[2];
    dest.v[0] = nx;
    dest.v[1] = ny;
    dest.v[2] = nz;
  }

  /** Multiply another 4x1 matrix with this one (full matrix) */
  public void mult(Vector4 dest) {
    float nx, ny, nz, nw;
    nx = m[0+0*4] * dest.v[0] + m[1+0*4] * dest.v[1] + m[2+0*4] * dest.v[2] + m[3+0*4] * dest.v[3];
    ny = m[0+1*4] * dest.v[0] + m[1+1*4] * dest.v[1] + m[2+1*4] * dest.v[2] + m[3+1*4] * dest.v[3];
    nz = m[0+2*4] * dest.v[0] + m[1+2*4] * dest.v[1] + m[2+2*4] * dest.v[2] + m[3+2*4] * dest.v[3];
    nw = m[0+3*4] * dest.v[0] + m[1+3*4] * dest.v[1] + m[2+3*4] * dest.v[2] + m[3+3*4] * dest.v[3];
    dest.v[0] = nx;
    dest.v[1] = ny;
    dest.v[2] = nz;
    dest.v[3] = nw;
  }

  public float get(int i, int j) {return m[j + i * 4];}
  public void set(int i, int j, float v) {m[j + i * 4] = v;}

  /** Transpose this matrix in place. */
  public void transpose() {
    float t;
    t = get(0, 1);
    set(0, 1, get(1, 0));
    set(1, 0, t);

    t = get(0, 2);
    set(0, 2, get(2, 0));
    set(2, 0, t);

    t = get(1, 2);
    set(1, 2, get(2, 1));
    set(2, 1, t);
  }

  /** Return the determinant. Computed across the zeroth row. */
  public float determinant() {
    return (get(0, 0) * (get(1, 1) * get(2, 2) - get(2, 1) * get(1, 2)) +
            get(0, 1) * (get(2, 0) * get(1, 2) - get(1, 0) * get(2, 2)) +
            get(0, 2) * (get(1, 0) * get(2, 1) - get(2, 0) * get(1, 1)));
  }

  /** Full matrix inversion in place. If matrix is singular, returns
      false and matrix contents are untouched. If you know the matrix
      is orthonormal, you can call transpose() instead. */
  public boolean invert() {
    float det = determinant();
    if (det == 0.0f) return false;

    // Form cofactor matrix
    mat.set(0, 0, get(1, 1) * get(2, 2) - get(2, 1) * get(1, 2));
    mat.set(0, 1, get(2, 0) * get(1, 2) - get(1, 0) * get(2, 2));
    mat.set(0, 2, get(1, 0) * get(2, 1) - get(2, 0) * get(1, 1));
    mat.set(1, 0, get(2, 1) * get(0, 2) - get(0, 1) * get(2, 2));
    mat.set(1, 1, get(0, 0) * get(2, 2) - get(2, 0) * get(0, 2));
    mat.set(1, 2, get(2, 0) * get(0, 1) - get(0, 0) * get(2, 1));
    mat.set(2, 0, get(0, 1) * get(1, 2) - get(1, 1) * get(0, 2));
    mat.set(2, 1, get(1, 0) * get(0, 2) - get(0, 0) * get(1, 2));
    mat.set(2, 2, get(0, 0) * get(1, 1) - get(1, 0) * get(0, 1));

    // Now copy back transposed
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        set(i, j, mat.get(j, i) / det);
    return true;
  }

  public void reverseTranslate() {
    m[0+3*4] *= -1.0f;
    m[1+3*4] *= -1.0f;
    m[2+3*4] *= -1.0f;
  }

  public void frustum(float left, float right, float bottom, float top, float znear, float zfar) {
    float temp, temp2, temp3, temp4;
    temp = 2.0f * znear;
    temp2 = right - left;
    temp3 = top - bottom;
    temp4 = zfar - znear;
    m[0] = temp / temp2;
    m[1] = 0.0f;
    m[2] = 0.0f;
    m[3] = 0.0f;

    m[4] = 0.0f;
    m[5] = temp / temp3;
    m[6] = 0.0f;
    m[7] = 0.0f;

    m[8] = (right + left) / temp2;
    m[9] = (top + bottom) / temp3;
    m[10] = (-zfar - znear) / temp4;
    m[11] = -1.0f;

    m[12] = 0.0f;
    m[13] = 0.0f;
    m[14] = (-temp * zfar) / temp4;
    m[15] = 0.0f;
  }

  private float degToRad(float x) {
    return (float)(x * (Math.PI/180.0f));
  }

  public void perspective(float fovyInDegrees, float aspectRatio, float znear, float zfar)
  {
    float ymax, xmax;
    ymax = (float)(znear * Math.tan(degToRad(fovyInDegrees) / 2.0f));
    xmax = ymax * aspectRatio;
    frustum(-xmax, xmax, -ymax, ymax, znear, zfar);
  }

  public void ortho(float left, float right, float bottom, float top, float near, float far) {
    float w = right - left, h = top - bottom, d = far - near;
    m[0] = 2 / w;
    m[1] = 0;
    m[2] = 0;
    m[3] = 0;

    m[4] = 0;
    m[5] = 2 / h;
    m[6] = 0;
    m[7] = 0;

    m[8] = 0;
    m[9] = 0;
    m[10] = -2 / d;  //why negative ???
    m[11] = 0;

    m[12] = -(left + right) / w;
    m[13] = -(top + bottom) / h;
    m[14] = -(far + near) / d;
    m[15] = 1;
  }

  /**
   * Sets the matrix to look at a point from a specified point.
   * Note:input vectors are clobbered.
   *
   * @param eye = camera position
   * @param at = point to look at
   * @param up = camera up vector (usually 0,1,0)
   */
  public void lookAt(Vector3 eye, Vector3 at, Vector3 up) {
    //see https://www.opengl.org/archives/resources/faq/technical/lookat.cpp
    at.v[0] -= eye.v[0];
    at.v[1] -= eye.v[1];
    at.v[2] -= eye.v[2];
    at.normalize();
    vec.cross(at, up);
    vec.normalize();
    up.cross(vec, at);
    at.scale(-1f);
    //right vector
    m[0] = vec.v[0];
    m[1] = vec.v[1];
    m[2] = vec.v[2];
    m[3] = 0;
    //up vector
    m[4] = up.v[0];
    m[5] = up.v[1];
    m[6] = up.v[2];
    m[7] = 0;
    //lookAt vector
    m[8] = at.v[0];
    m[9] = at.v[1];
    m[10] = at.v[2];
    m[11] = 0;
    //camera translation
    m[12] = eye.v[0];
    m[13] = eye.v[1];
    m[14] = eye.v[2];
    m[15] = 1f;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for(int a=0;a<16;a+=4) {
      if (a > 0) sb.append(",");
      sb.append(String.format("%.3f,%.3f,%.3f,%.3f", m[a + 0], m[a + 1], m[a + 2], m[a + 3]));
    }
    sb.append("]");
    return sb.toString();
  }
}

/*

Identity (.=never used - always zero)

1 0 0 .
0 1 0 .
0 0 1 .
0 0 0 1

Translation

1 0 0 0
0 1 0 0
0 0 1 0
x y z 1

Rotation (c1 = cy+cz; c2 = cx+cz; c3 = cx+cy)

 c1  zs -ys  0  =  right vector
-zs  c2  xs  0  =  up vector
 ys -xs  c3  0  =  -lookAt vector
 0   0   0   1

Scaling

x 0 0 0
0 y 0 0
0 0 z 0
0 0 0 1

*/
