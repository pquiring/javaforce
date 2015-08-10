import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.gl.*;
import static javaforce.gl.GL.*;

/**
 *
 * @author pquiring
 *
 * Created Sept 18, 2013
 */

public class GLCode {
  java.util.Timer glTimer, fpsTimer;
  final Object fpsLock = new Object();
  int fpsCounter;

  final int FPS = 65;

  GLScene scene = new GLScene();
  GLRender render = new GLRender();
  GLModel mod;
  GLObject box;

  boolean keys[] = new boolean[1024];

  final float speedMove = 2.0f;
  final float speedRotate = 5.0f;

  float alpha = 0.5f, alphadir = -0.01f;

  boolean doSwap = false;

  public GLCode(boolean doSwap) {
    this.doSwap = doSwap;
  }

//interface GLInterface
  public void init() {
    scene.texturePath = "./";

    System.out.println("GL Version=" + glGetString(GL.GL_VERSION));
    int glver[] = getVersion();
    if (glver[0] < 2) {
      JF.showError("Error", "OpenGL Version < 2.0 : Detected : " + glver[0] + "." + glver[1]);
      System.exit(0);
    }

    createWorld();

    //setup timers
    glTimer = new java.util.Timer();
    int delay = 1000 / FPS;
    glTimer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        frame();
      }
    }, delay, delay);
    fpsTimer = new java.util.Timer();
    fpsTimer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        int cnt;
        synchronized(fpsLock) {
          cnt = fpsCounter;
          fpsCounter = 0;
        }
        JFLog.log("fps=" + cnt);
//        JFLog.log("camera=" + scene.m_camera.toString());
//        JFLog.log("model=" + scene.m_model.toString());
      }
    }, 1000, 1000);

  }
  public void render() {
    synchronized(fpsLock) {
      fpsCounter++;
    }
    processMovement();
    render.render();
    if (doSwap) Main.swap();  //only swap with GLCanvas, not GLJPanel
  }
  public void resize(int width, int height) {
    render.resize(width, height);
  }

  public void frame() {
    box.m.addRotate(3.0f, 1.0f, 0.0f, 0.0f);
    box.m.addRotate(2.0f, 0.0f, 1.0f, 0.0f);
    box.m.addRotate(1.0f, 0.0f, 0.0f, 1.0f);
  }

  public void createWorld() {
    float x, y, z;
    int width = 512;
    int height = 512;
    System.out.println("size=" + width + "," + height);
    scene.init(GLVertexShader.source, GLFragmentShader.source);
    render.init(scene, width, height);
    resetPosition();

    x = -0.5f;
    y = -0.5f;
    z = -0.5f;

    box = new GLObject();
    GLUVMap map = box.createUVMap();
    map.textureIndex = 0;

    mod = new GLModel();
    mod.addObject(box);
    mod.addTexture("opengl.png");
    scene.addModel(mod);
    //create a box
    makeWall(x,y,z,1, box);
    makeWall(x,y,z,2, box);
    makeWall(x,y,z,3, box);
    makeWall(x,y,z,4, box);
    makeWall(x,y,z,5, box);
    makeWall(x,y,z,6, box);
    if (!scene.loadTextures()) {
      JFLog.log("Failed to load all textures");
      System.exit(0);
    }
    box.copyBuffers();
  }

  public GLObject makeWall(float x,float y,float z,int side,GLObject obj) {
    //use counter clock wise triangles
    float vp[];  //vertex coords (positions)
    vp = new float[] {
      (x     ) * 10.0f, (y     ) * 10.0f,  (z     ) * 10.0f,
      (x+1.0f) * 10.0f, (y     ) * 10.0f,  (z     ) * 10.0f,
      (x     ) * 10.0f, (y+1.0f) * 10.0f,  (z     ) * 10.0f,
      (x+1.0f) * 10.0f, (y+1.0f) * 10.0f,  (z     ) * 10.0f,
      (x     ) * 10.0f, (y     ) * 10.0f,  (z+1.0f) * 10.0f,
      (x+1.0f) * 10.0f, (y     ) * 10.0f,  (z+1.0f) * 10.0f,
      (x     ) * 10.0f, (y+1.0f) * 10.0f,  (z+1.0f) * 10.0f,
      (x+1.0f) * 10.0f, (y+1.0f) * 10.0f,  (z+1.0f) * 10.0f
    };
    int off = obj.vpl.size() / 3;  //current vertex count
    int pts[] = null;
    float uv[] = new float[8 * 2];
    switch (side) {
      case 1:  //top
        pts = new int[] {2,6,7,3};
        break;
      case 2:  //bottom
        pts = new int[] {5,4,0,1};
        break;
      case 3:  //left
        pts = new int[] {2,0,4,6};
        break;
      case 4:  //right
        pts = new int[] {7,5,1,3};
        break;
      case 5:  //front
        pts = new int[] {6,4,5,7};
        break;
      case 6:  //back
        pts = new int[] {3,1,0,2};
        break;
    }
    float u = 0.0f;
    float v = 0.0f;
    for(int a=0;a<4;a++) {
      uv[pts[a] * 2 + 0] = u;
      uv[pts[a] * 2 + 1] = v;
      if (u == 0.0f && v == 0.0f) v = 1.0f;
      else if (u == 0.0f && v == 1.0f) u = 1.0f;
      else if (u == 1.0f && v == 1.0f) v = 0.0f;
//      else if (tx == 0.0f && ty == 0.0f) tx = 0.0f;  //not needed - end of loop
    }
    obj.addVertex(vp, uv);
    obj.addPoly(new int[] {off + pts[0], off + pts[1], off + pts[2]});
    obj.addPoly(new int[] {off + pts[0], off + pts[2], off + pts[3]});
    return obj;
  }
  GLVector3 viewpoint = new GLVector3();
  GLVector3 uppoint = new GLVector3();
  GLVector3 leftpoint = new GLVector3();
  GLVector3 boxCenter = new GLVector3();
  public void processMovement() {
    viewpoint.set(0.0f, 0.0f, -1.0f);  //normally looking into monitor
    render.m_camera.mult(viewpoint);
    uppoint.set(0.0f, 1.0f, 0.0f);  //normally up is along y axis
    render.m_camera.mult(uppoint);
    leftpoint.set(-1.0f, 0.0f, 0.0f);  //normally left is along x axis
    render.m_camera.mult(leftpoint);

    if (keys[KeyEvent.VK_LEFT]) { rotateLR(-1.0f); }
    if (keys[KeyEvent.VK_RIGHT]) { rotateLR(1.0f); }
    if (keys[KeyEvent.VK_UP]) { rotateUD(1.0f); }
    if (keys[KeyEvent.VK_DOWN]) { rotateUD(-1.0f); }
    if (keys[KeyEvent.VK_Z]) { spin(1.0f); }
    if (keys[KeyEvent.VK_X]) { spin(-1.0f); }

    if (keys[KeyEvent.VK_Q]) { resetPosition(); }

    if (keys[KeyEvent.VK_F1]) { render.cameraRotate(10.0f, 1.0f, 0.0f, 0.0f); }
    if (keys[KeyEvent.VK_F2]) { render.cameraRotate(10.0f, 0.0f, 1.0f, 0.0f); }
    if (keys[KeyEvent.VK_F3]) { render.cameraRotate(10.0f, 0.0f, 0.0f, 1.0f); }
    if (keys[KeyEvent.VK_F5]) { render.cameraRotate(-10.0f, 1.0f, 0.0f, 0.0f); }
    if (keys[KeyEvent.VK_F6]) { render.cameraRotate(-10.0f, 0.0f, 1.0f, 0.0f); }
    if (keys[KeyEvent.VK_F7]) { render.cameraRotate(-10.0f, 0.0f, 0.0f, 1.0f); }

    if (keys[KeyEvent.VK_9]) { render.fovy -= 10.0f; System.out.println("fovy = " + render.fovy); }
    if (keys[KeyEvent.VK_0]) { render.fovy += 10.0f; System.out.println("fovy = " + render.fovy); }

    if (keys[KeyEvent.VK_S]) { move(1); }
    if (keys[KeyEvent.VK_W]) { move(-1); }
    if (keys[KeyEvent.VK_D]) { stride(1); }
    if (keys[KeyEvent.VK_A]) { stride(-1); }

    //fade box in/out
    alpha += alphadir;
    if (alpha < 0.0f) {
      alpha = 0.0f;
      alphadir *= -1.0f;
    } else if (alpha > 0.5f) {
      alpha = 0.5f;
      alphadir *= -1.0f;
    }
    box.color[3] = alpha;
  }
  public void rotateLR(float dir) {
    render.cameraRotate(speedRotate * uppoint.v[0], dir, 0.0f, 0.0f);
    render.cameraRotate(speedRotate * uppoint.v[1], 0.0f, dir, 0.0f);
    render.cameraRotate(speedRotate * uppoint.v[2], 0.0f, 0.0f, dir);
  }
  public void rotateUD(float dir) {
    render.cameraRotate(speedRotate * leftpoint.v[0], dir, 0.0f, 0.0f);
    render.cameraRotate(speedRotate * leftpoint.v[1], 0.0f, dir, 0.0f);
    render.cameraRotate(speedRotate * leftpoint.v[2], 0.0f, 0.0f, dir);
  }
  public void spin(float dir) {
    render.cameraRotate(speedRotate * viewpoint.v[0], dir, 0.0f, 0.0f);
    render.cameraRotate(speedRotate * viewpoint.v[1], 0.0f, dir, 0.0f);
    render.cameraRotate(speedRotate * viewpoint.v[2], 0.0f, 0.0f, dir);
  }
  public void move(float dir) {
    render.modelTranslate(
      viewpoint.v[0] * speedMove * dir,
      viewpoint.v[1] * speedMove * dir,
      viewpoint.v[2] * speedMove * dir
    );
  }
  public void stride(float dir) {
    render.modelTranslate(
      leftpoint.v[0] * speedMove * dir,
      leftpoint.v[1] * speedMove * dir,
      leftpoint.v[2] * speedMove * dir
    );
  }
  public void resetPosition() {
    render.cameraReset();
    render.modelReset();
    render.modelTranslate(0.0f, 0.0f, -20.0f);
  }
}
