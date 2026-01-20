package javaforce.gl;

/**
 * UV Map - 2D Texture coordinates (see Object3)
 *
 * @author pquiring
 */

import javaforce.*;
import static javaforce.gl.GL.*;

public class UVMap {
  public JFArrayFloat uvl;  //texture coords list (UV)
  public String name;
  public int textureIndex = -1;
  public boolean texloaded = false;
  public int uvb = -1;  //GL buffer
  public int idx;  //map index

  public UVMap(int idx) {
    uvl = new JFArrayFloat();
    this.idx = idx;
    if (idx > 1) {
      System.out.println("GLUVMap:Warning:More than 2 UVMaps not supported");
    }
  }

  public void addText(float[] uv) {
    uvl.append(uv);
  }

  public void copyBuffers() {
    GL gl = GL.getInstance();
    int[] ids = new int[1];
    if (uvb == -1) {
      gl.glGenBuffers(1, ids);
      uvb = ids[0];
      if (debug) {
        JFLog.log("GLUVMap:uvb=" + uvb);
      }
    }
    if (debug) {
      JFLog.log("GLUVMap.copyBuffer:" + uvb + "," + idx);
    }
    gl.glBindBuffer(GL_ARRAY_BUFFER, uvb);
    gl.glBufferData(GL_ARRAY_BUFFER, uvl.size() * 4, uvl.toArray(), GL_STATIC_DRAW);
  }

  public void bindBuffers(Scene scene) {
    GL gl = GL.getInstance();
    if (debug) {
      JFLog.log("GLUVMap.bindBuffer:" + uvb + "," + idx);
    }
    gl.glBindBuffer(GL_ARRAY_BUFFER, uvb);
    gl.glVertexAttribPointer(scene.tca[idx], 2, GL_FLOAT, GL_FALSE, 0, 0);
  }

  public void freeBuffers() {
    GL gl = GL.getInstance();
    int[] ids = new int[1];
    if (uvb != -1) {
      ids[0] = uvb;
      gl.glDeleteBuffers(1, ids);
      uvb = -1;
    }
  }
}
