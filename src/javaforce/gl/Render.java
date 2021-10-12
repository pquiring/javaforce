package javaforce.gl;

/**
 * Renders a view of a Scene
 *
 * @author pquiring
 */

import static javaforce.gl.GL.*;

public class Render {
  private int iwx, iwy; //window size (int)
  private float dwx, dwy; //window size (float)
  private float ratio;  //dwx / dwy
  private Scene scene;

  public Matrix m_camera;  //camera matrix (rotation/translation)
  public Matrix m_model;  //model matrix (translation only)

  public float fovy = 60.0f;
  public float zNear = 0.1f;  //Do NOT use zero!!!
  public float zFar = 10000.0f;

  public void init(Scene scene, int width, int height) {
    this.scene = scene;
    resize(width, height);
    reset();
  }
  public void resize(int width, int height) {
    iwx = width;
    iwy = height;
    dwx = (float)width;
    dwy = (float)height;
    ratio = dwx/dwy;
  }

  public void reset() {
    m_camera = new Matrix();
    m_model = new Matrix();
  }

  public void setFOV(float fov) {
    fovy = fov;
  }

  public void setRenderDistance(float near, float far) {
    zNear = near;
    zFar = far;
  }

  public void render() {
    scene.initTextures();
    Model mod;
    Object3 obj;
    Matrix mat = new Matrix();
    //setup camera view
    glViewport(0, 0, iwx, iwy);
    //setup model view
    //setup background clr
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    //render models
    int size_ml = scene.ml.size();
    mat.setIdentity();
    mat.perspective(fovy, ratio, zNear, zFar);
    glUniformMatrix4fv(scene.mpu, 1, GL_FALSE, mat.m);  //perspective matrix
    for(int a=0;a<size_ml;a++) {
      mod = scene.ml.get(a);
      if (!mod.visible) continue;
      mat.setIdentity();
      mat.mult4x4(m_camera);
      glUniformMatrix4fv(scene.mvu, 1, GL_FALSE, mat.m);  //view matrix
      int size_ol = mod.ol.size();
      for(int b=0;b<size_ol;b++) {
        obj = mod.ol.get(b);
        if (!obj.visible) continue;
        if (obj.needCopyBuffers) {
          obj.copyBuffers();
        }
        //need to optz this
        mat.setIdentity();
        mat.mult4x4(m_model);
        mat.mult4x4(mod.m);
        mat.mult4x4(obj.m);
        glUniformMatrix4fv(scene.mmu, 1, GL_FALSE, mat.m);  //model matrix

        for(int m=0;m<obj.maps.size();m++) {
          UVMap map = obj.maps.get(m);
          map.bindBuffers(scene);
          Texture tex = null;
          if ((map.textureIndex != -1) && (map.texloaded)) {
            tex = scene.tl.get(mod.getTexture(map.textureIndex));
            if (tex != null && tex.loaded) {
              tex.bind();
            } else {
              tex = null;
            }
          }
          if (tex == null) {
            System.out.println("GLRender:Warning:using blank texture, missing texture?");
            scene.blankTexture.bind();
          }
        }
        obj.bindBuffers(scene);
        obj.render(scene);
      }
    }
    glFlush();
  }
  public void cameraReset() {
    m_camera.setIdentity();
  }
  public void cameraSet(float angle, float ax, float ay, float az, float tx, float ty, float tz) {
    m_camera.setAATranslate(angle, ax, ay, az, tx, ty, tz);
  }
  public void cameraRotate(float angle, float ax, float ay, float az) {
    m_camera.addRotate(angle, ax, ay, az);
  }
  public void cameraTranslate(float tx, float ty, float tz) {
    m_camera.addTranslate(tx, ty, tz);
  }
  public void modelReset() {
    m_model.setIdentity();
  }
/*
  public void modelSet(float angle, float rx, float ry, float rz, float tx, float ty, float tz) {
    m_model.setAATranslate(angle, rx, ry, rz, tx, ty, tz);
  }
  public void modelRotate(float angle, float ax, float ay, float az) {
    m_model.addRotate(angle, ax, ay, az);
  }
*/
  public void modelTranslate(float tx, float ty, float tz) {
    m_model.addTranslate(tx, ty, tz);
  }
}
