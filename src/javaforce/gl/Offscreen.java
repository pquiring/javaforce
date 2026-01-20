package javaforce.gl;

/**
 * GL Offscreen rendering buffer.
 */

import java.awt.Image;

import javaforce.*;
import javaforce.awt.*;

import static javaforce.gl.GL.*;

public class Offscreen {
  //offscreen data
  private int os_fb, os_clr_rb, os_depth_rb;
  private int os_width, os_height;
  private int[] os_px;  //pixels
  private int[] os_fpx;  //flipped pixels
  private JFImage os_img;  //basically a BufferedImage

  /** Get offscreen buffer in a java.awt.Image */
  public Image getOffscreen() {
    GL gl = GL.getInstance();
    //TODO : the image is trippled here - need to optimize!!!
    gl.glReadPixels(0, 0, os_width, os_height, GL_BGRA, GL_UNSIGNED_BYTE, os_px);
    //invert and fix alpha (pixel by pixel - slow - OUCH!)
    //OpenGL makes black pixels transparent which causes unwanted trailing effects
    int src = (os_height - 1) * os_width;
    int dst = 0;
    for(int y=0;y<os_height;y++) {
      for(int x=0;x<os_width;x++) {
        os_fpx[dst++] = os_px[src++] | 0xff000000;
      }
      src -= os_width * 2;
    }
    os_img.putPixels(os_fpx, 0, 0, os_width, os_height, 0);
    return os_img.getImage();
  }

  /** Get offscreen buffer pixels (leaving alpha channel as is)
   * Pixels that are not rendered to are usually transparent.
   */
  public int[] getOffscreenPixels() {
    GL gl = GL.getInstance();
    gl.glReadPixels(0, 0, os_width, os_height, GL_BGRA, GL_UNSIGNED_BYTE, os_px);
    //invert and fix alpha (pixel by pixel - slow - OUCH!)
    //OpenGL makes black pixels transparent which causes unwanted trailing effects
    int src = (os_height - 1) * os_width;
    int dst = 0;
    for(int y=0;y<os_height;y++) {
      System.arraycopy(os_px, src, os_fpx, dst, os_width);
      src -= os_width;
      dst += os_width;
    }
    return os_fpx;
  }

  private void createBuffers(int width, int height) {
    GL gl = GL.getInstance();
    int[] ids = new int[1];
    gl.glGenRenderbuffers(1, ids);
    os_clr_rb = ids[0];
    if (debug) {
      JFLog.log("GLOffscreen:os_clr_rb=" + os_clr_rb);
    }
    gl.glBindRenderbuffer(GL_RENDERBUFFER, os_clr_rb);
    gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, width, height);
    gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, os_clr_rb);

    gl.glGenRenderbuffers(1, ids);
    os_depth_rb = ids[0];
    if (debug) {
      JFLog.log("GLOffscreen:os_depth_rb=" + os_depth_rb);
    }
    gl.glBindRenderbuffer(GL_RENDERBUFFER, os_depth_rb);
    gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, width, height);
    gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, os_depth_rb);
  }

  /** Resize offscreen buffer dimensions. */
  public void resizeOffscreen(int width, int height) {
    GL gl = GL.getInstance();
    os_width = width;
    os_height = height;
    os_px = new int[os_width * os_height];
    os_fpx = new int[os_width * os_height];
    os_img = new JFImage(os_width, os_height);

    gl.glBindFramebuffer(GL_FRAMEBUFFER, os_fb);

    int[] ids = {os_clr_rb, os_depth_rb};
    gl.glDeleteRenderbuffers(2, ids);
    createBuffers(width, height);
  }

  /** Creates an offscreen buffer to where rendering is directly to. */
  public void createOffscreen(int width, int height) {
    GL gl = GL.getInstance();
    if (os_fb != 0) return;  //already done
    int[] ids = new int[1];

    gl.glGenFramebuffers(1, ids);
    os_fb = ids[0];
    gl.glBindFramebuffer(GL_FRAMEBUFFER, os_fb);

    createBuffers(width, height);

    gl.glBindFramebuffer(GL_FRAMEBUFFER, os_fb);

    os_width = width;
    os_height = height;
    os_px = new int[os_width * os_height];
    os_fpx = new int[os_width * os_height];
    os_img = new JFImage(os_width, os_height);
  }

  public void setRenderToOffscreen(boolean state) {
    GL gl = GL.getInstance();
    gl.glBindFramebuffer(GL_FRAMEBUFFER, state ? os_fb : 0);
  }
}
