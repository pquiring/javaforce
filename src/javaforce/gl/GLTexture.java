package javaforce.gl;

import java.io.*;

import javaforce.*;
import javaforce.ui.*;
import static javaforce.gl.GL.*;

/** Stores a texture (image).
 * Textures are usually loaded after a new model is loaded.
 * All model's share the same set of textures. */

public class GLTexture {
  public Image image;
  public int refcnt;
  public int tid;
  public boolean loaded;
  public int idx;  //GL texture unit
  public String name;

  private static boolean mipmaps = false;

  public GLTexture(int idx) {
    refcnt = 0;
    tid = -1;
    image = new Image();
    loaded = false;
    this.idx = idx;
  }

  public GLTexture(int idx, int width, int height) {
    refcnt = 0;
    tid = -1;
    image = new Image(width, height);
    loaded = false;
    this.idx = idx;
  }

  public Image getImage() {
    return image;
  }

  public int getWidth() {
    return image.getWidth();
  }

  public int getHeight() {
    return image.getHeight();
  }

  public void set(int pixels[], int x, int y) {
    image.setSize(x,y);
    image.putPixels(pixels, 0, 0, x, y, 0);
  }

  public void setImage(Image img) {
    image = img;
  }

  public boolean loadPNG(String filename) {
    JFLog.log("Loading Texture:" + filename.toString());
    try {
      return loadPNG(new FileInputStream(filename));
    } catch (FileNotFoundException e) {
      JFLog.log("File not found:" + filename);
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean loadPNG(InputStream is) {
    if (!image.loadPNG(is)) {
      return false;
    }
    return true;
  }

  public boolean load() {
    if (tid == -1) {
      int[] id = new int[1];
      id[0] = -1;
      glGenTextures(1, id);
      if (id[0] == -1) {
        JFLog.log("glGenTextures failed:Error=0x" + Integer.toString(glGetError(), 16));
        return false;
      }
      tid = id[0];
      if (debug) {
        JFLog.log("GLTexture:id=" + tid);
      }
    }
    if (loaded) {
      return true;
    }
    glActiveTexture(GL_TEXTURE0 + idx);
    glBindTexture(GL_TEXTURE_2D, tid);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    if (mipmaps) {
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST_MIPMAP_NEAREST);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
    } else {
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    }
    if (debug) {
      JFLog.log("GLTexture.load:" + image.getWidth() + "x" + image.getHeight());
    }
    glTexImage2D(GL_TEXTURE_2D, 0, 4, image.getWidth(), image.getHeight(), 0, GL_BGRA
      , GL_UNSIGNED_BYTE, image.getBuffer());
    loaded = true;
    return true;
  }

  public void unload() {
    if (tid == -1) return;
    int[] id = new int[1];
    id[0] = tid;
    glDeleteTextures(1, id);
    tid = -1;
  }

  public void bind() {
    if (debug) {
      JFLog.log("GLTexture.bind:" + tid + "," + idx);
    }
    glActiveTexture(GL_TEXTURE0 + idx);
    glBindTexture(GL_TEXTURE_2D, tid);
  }
}
