package javaforce.gl;

import javaforce.gl.model.GL_JF3D;
import javaforce.gl.model.GL_BLEND;
import javaforce.gl.model.GL_3DS;
import java.util.*;

import javaforce.*;
import static javaforce.gl.GL.*;

/** Scene is a primitive 3D framework.
 * Holds all loaded 3D meshes and related resources.
 */

public class Scene {
  private static final boolean DEBUG = false;

  private boolean needinittex = true;

  ArrayList<Model> ml;
  HashMap<String, Texture> tl; //texture list
  HashMap<String, Model> mtl; //model templates list

  private ArrayList<Integer> freeglidlist;

  Texture blankTexture;

  public Scene() {
    freeglidlist = new ArrayList<Integer>();
    reset();
    texturePath = "";
    blankTexture = new Texture(0);
    blankTexture.set(new int[] {-1},1,1);  //white pixel
  }

  public boolean inited = false;

  public String texturePath;

  public int fragShader, vertexShader, program;
  public int vpa;  //attribs
  public int tca[] = new int[2];
  public int uUVMaps;
  public int mpu, mmu, mvu;  //uniform matrix'es (perspective, model, view)

  public void init(String vertex, String fragment) {  //must give size of render window
    glFrontFace(GL_CCW);  //3DS uses GL_CCW
    glEnable(GL_CULL_FACE);  //don't draw back sides
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);

    vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader, 1, new String[] {vertex}, null);
    glCompileShader(vertexShader);
    if (debug) JFLog.log("vertex log=" + glGetShaderInfoLog(vertexShader));

    fragShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragShader, 1, new String[] {fragment}, null);
    glCompileShader(fragShader);
    if (debug) JFLog.log("fragment log=" + glGetShaderInfoLog(fragShader));

    program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragShader);
    glLinkProgram(program);
    if (debug) JFLog.log("program log=" + glGetProgramInfoLog(program));
    glUseProgram(program);

    vpa = glGetAttribLocation(program, "aVertexPosition");
    glEnableVertexAttribArray(vpa);

    tca[0] = glGetAttribLocation(program, "aTextureCoord1");
    glEnableVertexAttribArray(tca[0]);
    int uSampler1 = glGetUniformLocation(program, "uSampler1");
    glUniform1i(uSampler1, 0);

    tca[1] = glGetAttribLocation(program, "aTextureCoord2");
    glEnableVertexAttribArray(tca[1]);
    int uSampler2 = glGetUniformLocation(program, "uSampler2");
    glUniform1i(uSampler2, 1);

    mpu = glGetUniformLocation(program, "uPMatrix");
    mmu = glGetUniformLocation(program, "uMMatrix");
    mvu = glGetUniformLocation(program, "uVMatrix");
    uUVMaps = glGetUniformLocation(program, "uUVMaps");

    if (debug) {
      JFLog.log("attribs=" + vpa + "," + tca[0] + "," + tca[1]);
      JFLog.log("uniforms=" + mpu + "," + mmu + "," + mvu + "," + uUVMaps + "," + uSampler1 + "," + uSampler2);
    }

    initTextures();

    inited = true;
  }
  public void reset() {
    if (tl != null) releaseTextures();
    ml = new ArrayList<Model>();
    tl = new HashMap<String, Texture>();
    mtl = new HashMap<String, Model>();
  }
  private void releaseTextures() {
    Set keyset = tl.keySet();
    Iterator iter = keyset.iterator();
    String texidx;
    Texture tex;
    while (iter.hasNext()) {
      texidx = (String)iter.next();
      tex = tl.get(texidx);
      if (tex.tid != -1) {
        releaseTexture(tex.tid);
        tex.tid = -1;
      }
    }
  }
  private void releaseTexture(int glid) {
    freeglidlist.add(glid);
  }
//load textures from disk to general-purpose memory
  public boolean loadTextures() {
    //scan thru object list and load them all
    boolean ret = true;
    Object3 obj;
    Model mod;
    int modCnt = ml.size();
    for(int a=0;a<modCnt;a++) {
      mod = ml.get(a);
      int objCnt = mod.ol.size();
      for(int b=0;b<objCnt;b++) {
        obj = mod.ol.get(b);
        int mapCnt = obj.maps.size();
        for(int m=0;m<mapCnt;m++) {
          UVMap map = obj.maps.get(m);
          if (map.texloaded) continue;
          if (loadTexture(mod.getTexture(map.textureIndex), map.idx)) {
            map.texloaded = true;
          } else {
            ret = false;
          }
        }
      }
    }
    return ret;
  }
  private boolean loadTexture(String fn, int idx) {
    if (fn == null) return false;
    Texture tex;

    tex = tl.get(fn);
    if (tex != null) {
      tex.refcnt++;
      return true;
    }
    needinittex = true;
    tex = new Texture(idx);
    tex.name = fn;
    if (!tex.loadPNG(fn)) {
      JFLog.log("Error:Failed to load texture:" + fn);
      return false;
    }
    tex.refcnt = 1;
    tl.put(fn, tex);
    return true;
  }
  //directly load a texture
  public boolean setTexture(String fn, int[] px, int w, int h, int idx) {
    Texture tex = tl.get(fn);
    if (tex == null) {
      tex = new Texture(idx);
      tl.put(fn, tex);
    } else {
      tex.loaded = false;
    }
    tex.set(px, w, h);
    needinittex = true;
    return false;
  }
  //directly load a texture
  public boolean setTexture(String fn, Texture tex) {
    tl.put(fn, tex);
    needinittex = true;
    return false;
  }
//load textures into video memory (texture objects)
  boolean initTextures() {
    if (!needinittex) {
      return true;
    }
    //setup blankTexture
    if (blankTexture.tid == -1) initTexture(blankTexture);
    //first uninit any that have been deleted
    if (freeglidlist.size() > 0) uninitTextures();
    //scan thru object list and load them all
    Set keyset = tl.keySet();
    Iterator iter = keyset.iterator();
    String texidx;
    while (iter.hasNext()) {
      texidx = (String)iter.next();
      if (!initTexture(tl.get(texidx))) return false;
    }
    needinittex = false;
    return true;
  }
  private boolean initTexture(Texture tex) {
    return tex.load();
  }
  private boolean uninitTextures() {
    while (freeglidlist.size() > 0) {
      uninitTexture(freeglidlist.get(0));
      freeglidlist.remove(0);
    }
    return true;
  }
  private boolean uninitTexture(int glid) {
    int[] id = new int[1];
    id[0] = glid;
    glDeleteTextures(1, id);
    return true;
  }
  public void releaseUnusedTextures() {
    Set keyset = tl.keySet();
    Iterator iter = keyset.iterator();
    String texidx;
    while (iter.hasNext()) {
      texidx = (String)iter.next();
      if (tl.get(texidx).refcnt == 0) releaseTexture(tl.get(texidx).tid);
    }
  }
  /** Release a cloned model @ index.
   */
  public void releaseModel(int idx) {
    Model mod;
    Object3 obj;
    mod = ml.get(idx);
    int size = mod.ol.size();
    for(int a=0;a<size;a++) {
      obj = mod.ol.get(a);
      for(int m=0;m<obj.maps.size();m++) {
        UVMap map = obj.maps.get(m);
        tl.get(mod.getTexture(map.textureIndex)).refcnt--;
      }
    }
    ml.remove(idx);
  }
  public int modelCount() { return ml.size(); }
  public boolean addModel(Model mod) { return addModel(mod, 0); }  //places Model at start of list
  public boolean addModel(Model mod, int idx) { if (mod == null) return false; ml.add(idx, mod); return true;}  //place Models with transparent textures last
  public int indexOfModel(Model mod) { return ml.indexOf(mod); }
  public void removeModel(int idx) { ml.remove(idx); }
  public void removeModel(Model mod) { ml.remove(mod); }
  public void nextFrame(int objidx) { ml.get(objidx).nextFrame(); }
  public void setFrame(int objidx, int frame) { ml.get(objidx).setFrame(frame); }
  public void modelTranslate(int idx, float x, float y, float z) { ml.get(idx).translate(x,y,z); }
  public void modelRotate(int idx, float angle, float x, float y, float z) { ml.get(idx).rotate(angle,x,y,z); }
  public void modelScale(int idx, float x, float y, float z) { ml.get(idx).scale(x,y,z); }
  /** Loads a .3DS file into the template array.
   * Use addModel() to add a clone into the render scene.
   */
  public Model load3DS(String fn) {
    Model mod;

    mod = mtl.get(fn);
    if (mod != null) {
      mod.refcnt++;
      return mod;
    }

    GL_3DS loader = new GL_3DS();
    mod = loader.load(fn);
    if (mod == null) return null;
    mtl.put(fn, mod);
    mod.refcnt = 1;
    mod = (Model)mod.clone();

    return mod;
  }
  /** Loads a .blend file into the template array.
   * Use addModel() to add a clone into the render scene.
   */
  public Model loadBlend(String fn) {
    Model mod;

    mod = mtl.get(fn);
    if (mod != null) {
      mod.refcnt++;
      return mod;
    }

    GL_BLEND loader = new GL_BLEND();
    mod = loader.load(fn);
    if (mod == null) return null;
    mtl.put(fn, mod);
    mod.refcnt = 1;
    mod = (Model)mod.clone();

    return mod;
  }
  /** Loads a .JF3D file into the template array.
   * Use addModel() to add a clone into the render scene.
   */
  public Model loadJF3D(String fn) {
    Model mod;

    mod = mtl.get(fn);
    if (mod != null) {
      mod.refcnt++;
      return mod;
    }

    GL_JF3D loader = new GL_JF3D();
    mod = loader.load(fn);
    if (mod == null) return null;
    mtl.put(fn, mod);
    mod.refcnt = 1;
    mod = (Model)mod.clone();

    return mod;
  }
  /** Clones a pre-loaded model.
   * Use addModel() to add into the render scene.
   */
  public Model cloneModel(String fn) {
    Model mod = mtl.get(fn);
    if (mod == null) return null;
    mod.refcnt++;
    return (Model)mod.clone();
  }
  public void unloadModel(Model mod) {
    mod.refcnt--;
  }
  //this will release all unused models
  public void releaseModel() {
    Set keyset = mtl.keySet();
    Iterator iter = keyset.iterator();
    String idx;
    Model mod;
    while (iter.hasNext()) {
      idx = (String)iter.next();
      mod = mtl.get(idx);
      if (mod.refcnt == 0) {
        mtl.remove(idx);
      }
    }
  }
}
