/**
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javaforce.*;
import javaforce.gl.*;

public class ViewPanel extends JPanel implements GLInterface, MouseListener, MouseMotionListener, MouseWheelListener {
  public ViewPanel() {
    setLayout(new BorderLayout());
    canvas = new GLCanvas();
    add(canvas, BorderLayout.CENTER);
    canvas.addMouseListener(this);
    canvas.addMouseMotionListener(this);
    canvas.addMouseWheelListener(this);
  }

  public GLCanvas canvas;
  public GLRender render;
  private boolean inited;

  public void init(GL gl, Component comp) {
    render = new GLRender();
    render.init(Data.scene, getWidth(), getHeight());
    render.cameraTranslate(0, 0, -10);  //move back a bit
  }

  public void render(GL gl) {
    if (render == null) {
      gl.clear(0x000000, getWidth(), getHeight());
    } else {
      render.render(gl);
      gl.swap();
    }
  }

  public void resize(GL gl, int x, int y) {
    render.resize(x,y);
  }

  public void paint(Graphics g) {
    if (!inited) {
      //creates a shared context with main GLCanvas
      canvas.init(this, Data.canvas.getGL());
      inited = true;
    }
    super.paint(g);
  }

  public Dimension getMinimumSize() {
    return new Dimension(5,5);
  }

/*
  Blender controls:

  1 clk - move obj center pt

  2 drag - rotate camera view
     + c - zoom camera in/out
     + s - move camera view

  3 drag - move obj(s)
     + c - snap w/ moving
     + s - move slowly w/ moving

  wheel  - zoom camera in/out
     + c - move camera left/right
     + a - move camera up/down

  */

  public int b1x, b1y;  //button 1
  public int b2x, b2y;  //button 2
  public int b3x, b3y;  //button 3

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    if (e.getButton() == e.BUTTON2) {
      b2x = e.getX();
      b2y = e.getY();
    }
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mouseDragged(MouseEvent e) {
    if (e.getModifiers() == e.BUTTON2_MASK) {
      //rotate view
      int x = e.getX();
      int y = e.getY();
      int dx = x - b2x;
      int dy = y - b2y;
      render.cameraRotate(dx, 0,1,0);  //screen x = rotate on y
      render.cameraRotate(dy, 1,0,0);  //screen y = rotate on x
      b2x = x;
      b2y = y;
      canvas.repaint();
    }
  }

  public void mouseMoved(MouseEvent e) {
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    if (Data.scene == null) return;
    int scroll = e.getWheelRotation();  //1 or -1
    render.cameraTranslate(0, 0, scroll);
    canvas.repaint();
  }
}
