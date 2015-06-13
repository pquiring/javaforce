package javaforce.gl;

import java.util.*;

import javaforce.*;

/** <code>GLObject</code> consists of vertex points, and polygons (usually triangles).
 * All polygons share the same orientation (rotation, translation, scale).
 */

public class GLObject implements Cloneable {
  public JFArrayFloat vpl;  //vertex position list
  public JFArrayInt vil;  //vertex index list
  public int vpb = -1, vib = -1;  //GL Buffers

  public int type = GL.GL_TRIANGLES;  //GL_TRIANGLES or GL_QUADS

  public ArrayList<GLUVMap> maps = new ArrayList<GLUVMap>();

  public boolean visible = true;
  public boolean needCopyBuffers = true;
//animation data
  public HashMap<Integer, GLTranslate> tl;  //move list
  public HashMap<Integer, GLRotate> rl;  //rotation list
  public HashMap<Integer, GLScale> sl;  //scale list
  public int frameIndex;
  public GLMatrix m;  //current rotation, translation, scale
  public float color[];  //RGBA (default = 1.0f,1.0f,1.0f,1.0f)
  public GLVertex org;  //origin (default = 0.0f,0.0f,0.0f)
  public String name;
  public int parent;  //a 3ds file field (not used)
  public int maxframeCount;
  public GLObject() {
    frameIndex = 0;
    vpl = new JFArrayFloat();
    vil = new JFArrayInt();
    tl = new HashMap<Integer, GLTranslate>();
    rl = new HashMap<Integer, GLRotate>();
    sl = new HashMap<Integer, GLScale>();
    color = new float[4];
    for(int a=0;a<4;a++) color[a] = 1.0f;
    visible = true;
    org = new GLVertex();
    parent = -1;
    maxframeCount = 0;
    m = new GLMatrix();
  }
  public Object clone() {
    GLObject cln = new GLObject();
    cln.vpl = vpl;
    cln.vil = vil;
    cln.maps = maps;
    cln.visible = visible;
    cln.tl = tl;
    cln.rl = rl;
    cln.sl = sl;
    cln.frameIndex = frameIndex;
    cln.m = (GLMatrix)m.clone();  //super.clone() would use an assignment
    cln.color = new float[4];
    for(int a=0;a<4;a++) cln.color[a] = color[a];
    cln.org = org;
    cln.parent = parent;
    cln.maxframeCount = maxframeCount;
    cln.type = type;
    return cln;
  }
  public void setVisible(boolean state) {visible = state;}
  public void addRotate(float angle, float x, float y, float z, GLVertex org) {
    GLMatrix tmp = new GLMatrix();
    //rotates relative to org
    tmp.setAA(angle, x, y, z);  //set rotation
    tmp.addTranslate(org.x, org.y, org.z);  //set translation
    m.mult4x4(tmp);  //add it
    //now undo translation
    tmp.setIdentity3x3();  //remove rotation
    tmp.reverseTranslate();
    m.mult4x4(tmp);
  }
  public void addTranslate(float x, float y, float z) {
    m.addTranslate(x,y,z);
  }
  public void addScale(float x, float y, float z) {
    m.addScale(x,y,z);
  }
  public void setFrame(int idx) {  //0=init state
    GLRotate _r;
    GLTranslate _t;
    GLScale _s;
    frameIndex = idx;
    if (idx == 0) {
      m.setIdentity();
      return;
    }
    _t = tl.get(idx);
    if (_t != null) {
      addTranslate((_t.x - org.x),(_t.y - org.y),(_t.z - org.z));
    }
    _r = rl.get(idx);
    if (_r != null) {
      addRotate(_r.angle,_r.x,_r.y,_r.z,org);
    }
    _s = sl.get(idx);
    if (_s != null) {
      addScale(_s.x, _s.y, _s.z);
    }
  }
  public void nextFrame() {
    setFrame(frameIndex+1);
  }
  public int frameCount() {
    return maxframeCount;
  }
  public void addVertex(float xyz[]) {
    vpl.append(xyz);
  }
  public void addVertex(float xyz[], float uv[]) {
    vpl.append(xyz);
    maps.get(0).uvl.append(uv);
  }
  public void addVertex(float xyz[], float uv1[], float uv2[]) {
    vpl.append(xyz);
    maps.get(0).uvl.append(uv1);
    maps.get(1).uvl.append(uv2);
  }
  public void addVertex(GLVertex v) {
    vpl.append(v.x);
    vpl.append(v.y);
    vpl.append(v.z);
    GLUVMap map = maps.get(0);
    map.uvl.append(v.u);
    map.uvl.append(v.v);
  }
  public void addText(float uv[]) {
    maps.get(0).addText(uv);
  }
  public void addText(float uv[], int map) {
    maps.get(map).addText(uv);
  }
  public void addPoly(int pts[]) {
    vil.append(pts);
  }
  public void copyBuffers(GL gl) {
    int ids[] = new int[1];

    if (vpb == -1) {
      gl.glGenBuffers(1, ids);
      vpb = ids[0];
    }
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vpb);
    gl.glBufferData(GL.GL_ARRAY_BUFFER, vpl.size() * 4, vpl.toArray(), GL.GL_STATIC_DRAW);

    for(int a=0;a<maps.size();a++) {
      maps.get(a).copyBuffers(gl);
    }

    if (vib == -1) {
      gl.glGenBuffers(1, ids);
      vib = ids[0];
    }
    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vib);
    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, vil.size() * 4, vil.toArray(), GL.GL_STREAM_DRAW);
    needCopyBuffers = false;
  }
  public void bindBuffers(GLScene scene, GL gl) {
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vpb);
    gl.glVertexAttribPointer(scene.vpa, 3, GL.GL_FLOAT, GL.GL_FALSE, 0, 0);

    for(int m=0;m<maps.size();m++) {
      maps.get(m).bindBuffers(scene, gl);
    }

    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vib);
  }
  public void render(GLScene scene, GL gl) {
    if (vpl.size() == 0 || vil.size() == 0) return;  //crashes if empty ???
    int uvcnt = maps.size();
    gl.glUniform1i(scene.uUVMaps, uvcnt);
    GL.glEnableVertexAttribArray(scene.tca[0]);
    if (uvcnt > 1) {
      GL.glEnableVertexAttribArray(scene.tca[1]);
    } else {
      GL.glDisableVertexAttribArray(scene.tca[1]);
    }
    gl.glDrawElements(type, vil.size(), GL.GL_UNSIGNED_INT, 0);
  }
  public GLUVMap createUVMap() {
    GLUVMap map = new GLUVMap(maps.size());
    maps.add(map);
    return map;
  }
  public GLUVMap getUVMap(int idx) {
    return maps.get(idx);
  }
  public GLUVMap getUVMap(String name) {
    for(int a=0;a<maps.size();a++) {
      GLUVMap map = maps.get(a);
      if (map.name.equals(name)) return map;
    }
    return null;
  }
  public void print(GLModel model) {
    System.out.println("Object:" + name);
    //print vertex data
    float vp[] = vpl.toArray();
    for(int a=0;a<vp.length;) {
      System.out.println(String.format("v[%d]=%6.3f,%6.3f,%6.3f", a/3, vp[a++], vp[a++], vp[a++]));
    }
    //print poly data
    int vi[] = vil.toArray();
    for(int a=0;a<vi.length;) {
      switch (type) {
        case GL.GL_TRIANGLES:
          System.out.println(String.format("i[%d]=%d,%d,%d", a/3, vi[a++], vi[a++], vi[a++]));
          break;
        case GL.GL_QUADS:
          System.out.println(String.format("i[%d]=%d,%d,%d,%d", a/4, vi[a++], vi[a++], vi[a++], vi[a++]));
          break;
      }
    }
    //print uv maps
    for(int m=0;m<maps.size();m++) {
      GLUVMap map = maps.get(m);
      System.out.println("UVMap:" + map.name + ",texture=" + model.textures.get(map.textureIndex));
      float uv[] = map.uvl.toArray();
      for(int a=0;a<uv.length;) {
        System.out.println(String.format("uv[%d]=%6.3f,%6.3f", a/2, uv[a++], uv[a++]));
      }
    }
  }
}
