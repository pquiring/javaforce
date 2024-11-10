package javaforce.gl;

import java.util.*;

import javaforce.*;

/** <code>Model</code> is a set of <code>Object3</code>'s that all share the same base orientation (rotation, translation, scale)
 * Each object can also have its own orientation.
 *
 * See javaforce.gl.model for import/export classes.
 */

public class Model implements Cloneable {
  public ArrayList<Object3> ol;  //obj list
  public ArrayList<String> textures;
  public Matrix m;  //translation, rotation, scale matrix for all sub-objects
  public boolean visible = true;
  public int refcnt;

  public static boolean debug = false;

  public Model() {
    m = new Matrix();
    m.setIdentity();
    ol = new ArrayList<Object3>();
    textures = new ArrayList<String>();
  }
  private Model(Matrix m) {  //for clone()
    this.m = m;
    ol = new ArrayList<Object3>();
  }
  /**
  * Clones deep enough so that the cloned object will include seperate GLObjects, but share vertex, vertex point,
  * and animation data (except for the frame position).
  */
  public Object clone() {
    Model c = new Model((Matrix)m.clone());
    int objs = ol.size();
    for(int a=0;a<objs;a++) c.ol.add((Object3)ol.get(a).clone());
    c.textures = textures;
    return c;
  }
  public void setVisible(boolean state) {visible = state;}
  public void addObject(Object3 obj) {
    ol.add(obj);
  }
  public Object3 getObject(String name) {
    for(int a=0;a<ol.size();a++) {
      Object3 o = ol.get(a);
      if (o.name.equals(name)) {
        return o;
      }
    }
    if (debug) JFLog.log("Model:Could not find object:" + name);
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
    Object3 obj;
    int size = ol.size();
    for(int i=0;i<size;i++) {
      obj = ol.get(i);
      obj.nextFrame();
    }
  }
  public void setFrame(int idx) {
    Object3 obj;
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
