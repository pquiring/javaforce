package javaforce.gl;

import java.io.*;

import javaforce.*;

/** Stores a texture (image).
 * Textures are usually loaded after a new model is loaded.
 * All model's share the same set of textures. */

public class GLTexture {
  public JFImage bitmap;
  public int refcnt;
  public int glid;
  public boolean loaded;
  public int idx;  //GL texture unit
  public String name;

  private static boolean mipmaps = false;

  public GLTexture(int idx) {
    refcnt = 0;
    glid = -1;
    bitmap = new JFImage();
    loaded = false;
    this.idx = idx;
  }

  public void set(int pixels[], int x, int y) {
    bitmap.setSize(x,y);
    bitmap.putPixels(pixels, 0, 0, x, y, 0);
  }

  public boolean load(String filename) {
    JFLog.log("Loading Texture:" + filename.toString());
    try {
      return load(new FileInputStream(filename));
    } catch (FileNotFoundException e) {
      JFLog.log("File not found:" + filename);
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean load(InputStream is) {
    if (!bitmap.load(is)) {
      return false;
    }
/* //test
    int px[] = bitmap.getBuffer();
    java.util.Random r = new java.util.Random();
    for(int p=0;p<px.length;p++) {
      px[p] = r.nextInt() | 0xff000000;
    }
*/
    return true;
  }

  public boolean load(GL gl) {
    if (glid == -1) {
      int id[] = new int[1];
      id[0] = -1;
      gl.glGenTextures(1, id);
      if (id[0] == -1) {
        JFLog.log("glGenTextures failed:Error=0x" + Integer.toString(gl.glGetError(), 16));
        return false;
      }
      glid = id[0];
    }
    if (loaded) {
      return true;
    }
    gl.glActiveTexture(GL.GL_TEXTURE0 + idx);
    gl.glBindTexture(GL.GL_TEXTURE_2D, glid);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
    if (mipmaps) {
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST_MIPMAP_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST_MIPMAP_NEAREST);
    } else {
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    }
    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 4, bitmap.getWidth(), bitmap.getHeight(), 0, GL.GL_BGRA
      , GL.GL_UNSIGNED_BYTE, bitmap.getPixels());
    loaded = true;
    return true;
  }

  public void bind(GL gl) {
    gl.glActiveTexture(GL.GL_TEXTURE0 + idx);
    gl.glBindTexture(GL.GL_TEXTURE_2D, glid);
  }
}

