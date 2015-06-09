package javaforce.gl;

import java.util.*;
import java.io.*;

import javaforce.*;

/** <code>GLModel</code> is a set of <code>GLObject</code>'s that all share the same base orientation (rotation, translation, scale)
 * Each object can also have its own orientation in addition to this
 * Usually a 3DS file is loaded into one GLModel.
 * Each Object in the 3DS file will be stored into GLObject's.
 */

public class GLModel implements Cloneable {
  public ArrayList<GLObject> ol;  //obj list
  public ArrayList<String> textures;
  public GLMatrix m;  //translation, rotation, scale matrix for all sub-objects
  public boolean visible = true;
  public int refcnt;

  public GLModel() {
    m = new GLMatrix();
    m.setIdentity();
    ol = new ArrayList<GLObject>();
    textures = new ArrayList<String>();
  }
  private GLModel(GLMatrix m) {  //for clone()
    this.m = m;
    ol = new ArrayList<GLObject>();
  }
  /**
  * Clones deep enough so that the cloned object will include seperate GLObjects, but share vertex, vertex point,
  * and animation data (except for the frame position).
  */
  public Object clone() {
    GLModel c = new GLModel((GLMatrix)m.clone());
    int objs = ol.size();
    for(int a=0;a<objs;a++) c.ol.add((GLObject)ol.get(a).clone());
    c.textures = textures;
    return c;
  }
  public void setVisible(boolean state) {visible = state;}
  public void addObject(GLObject obj) {
    ol.add(obj);
  }
  public GLObject getObject(String name) {
    for(int a=0;a<ol.size();a++) {
      GLObject o = ol.get(a);
      if (o.name.equals(name)) {
        return o;
      }
    }
    JFLog.log("GLModel:Could not find object:" + name);
    return null;
  }
  public void setIdentity() {
    m.setIdentity();
  }
  //these are additive
  public void rotate(float angle, float x, float y, float z) {
    m.addRotate(angle, x, y, z);
  }
  public void translate(float x, float y, float z) {
    m.addTranslate(x, y, z);
  }
  public void scale(float x, float y, float z) {
    m.addScale(x, y, z);
  }
  public void nextFrame() {
    GLObject obj;
    int size = ol.size();
    for(int i=0;i<size;i++) {
      obj = ol.get(i);
      obj.nextFrame();
    }
  }
  public void setFrame(int idx) {
    GLObject obj;
    int size = ol.size();
    for(int i=0;i<size;i++) {
      obj = ol.get(i);
      obj.setFrame(idx);
    }
  }
  /** Adds a texture filename and returns index. */
  public int addTexture(String fn) {
    for(int a=0;a<textures.size();a++) {
      if (textures.get(a).equals(fn)) return a;
    }
    textures.add(fn);
    return textures.size() - 1;
  }
  public String getTexture(int idx) {
    return textures.get(idx);
  }
  public void print() {
    System.out.println("Model data");
    for(int a=0;a<ol.size();a++) {
      ol.get(a).print(this);
    }
  }
}
