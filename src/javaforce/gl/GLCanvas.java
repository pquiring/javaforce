package javaforce.gl;

/**
 * OpenGL rendering in a Canvas.
 *
 * Works on all platforms.  Recommended.
 *
 * @author pquiring
 *
 * Created : Sept 18, 2013
 */

import java.awt.*;
import javax.swing.*;

import javaforce.*;

public class GLCanvas extends Canvas {
  private GLInterface iface;
  private GL gl;

  public boolean init(GLInterface iface) {
    return init(iface, null);
  }
  public boolean init(GLInterface iface, GL shared) {
    this.iface = iface;
    gl = new GL(iface);
    if (!gl.createComponent(this, shared)) return false;
    gl.makeCurrent();
    gl.glInit();  //load GL functions if needed
    iface.init(gl, this);
    return true;
  }
  public void paint(Graphics g) {
    if (gl == null) return;
    gl.render();
  }
  public void update(Graphics g) {
    paint(g);
  }
  public void setSize(int w, int h) {
    super.setSize(w,h);
    if (iface != null) iface.resize(gl, w, h);
  }
  public void setSize(Dimension d) {
    super.setSize(d);
    if (iface != null) iface.resize(gl, d.width, d.height);
  }
  public void setBounds(int x,int y,int w,int h) {
    super.setBounds(x,y,w,h);
    if (iface != null) iface.resize(gl, w, h);
  }
  public void setBounds(Rectangle r) {
    super.setBounds(r);
    if (iface != null) iface.resize(gl, r.width, r.height);
  }
  public void destroy() {
    gl.destroy();
  }
  public GL getGL() {
    return gl;
  }
  public void addNotify() {
    JF.disableBackgroundErase(this);
    super.addNotify();
    JF.disableBackgroundErase(this);
  }
}
