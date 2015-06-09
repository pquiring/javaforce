import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Border extends JComponent implements MouseListener, MouseMotionListener, KeyListener {

  public static MainPanel panel;
  public int mx=0, my=0;  //current mouse pos
  public int sx=0, sy=0;  //starting draw pos
  public enum Types {east, south, corner};
  public Types borderType;
  public PaintCanvas pc;

  private void init() {
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    setFocusable(true);
  }

  public Border(PaintCanvas parent, Types bt) {
    this.borderType = bt;
    init();
    setName("border");
    pc = parent;
  }

  public void setImageSize(int x,int y) {
    if (x <= 0) x = 1;
    if (y <= 0) y = 1;

    setSize(x,y);

    setPreferredSize(new Dimension(x,y));
  }

  public void setSize(int x, int y) {
//    System.out.println("PaintCanvas.setSize:" + x + "," + y + ":" + getName());
    super.setSize(x,y);  //calls setBounds()
  }

  public void setSize(Dimension d) {
//    System.out.println("PaintCanvas.setSize:" + d + ":" + getName());
    super.setSize(d);  //calls setBounds()
  }

  public void setBounds(int x,int y,int w,int h) {
//    System.out.println("PaintCanvas.setBounds:" + x + "," + y + "," + w + "," + h + ":" + getName());
    super.setBounds(x,y,w,h);
  }

  public Dimension getPreferredSize() {
    Dimension ps = super.getPreferredSize();
//    System.out.println("PaintCanvas.getPreferredSize()" + ps);
    return ps;
  }

  public Dimension getMinimumSize() {
//    System.out.println("PaintCanvas.getMinimumSize()" + getPreferredSize());
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
//    System.out.println("PaintCanvas.getMaximumSize()" + getPreferredSize());
    return getPreferredSize();
  }

  public void mouseClicked(MouseEvent e) { panel.mouseClicked(e); }
  public void mouseEntered(MouseEvent e) { panel.mouseEntered(e); }
  public void mouseExited(MouseEvent e) { panel.mouseExited(e); }
  public void mousePressed(MouseEvent e) { panel.mousePressed(e); }
  public void mouseReleased(MouseEvent e) { panel.mouseReleased(e); }

  public void mouseDragged(MouseEvent e) { mx = e.getX(); my = e.getY(); panel.mouseDragged(e); }
  public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); panel.mouseMoved(e); }

  public void keyPressed(KeyEvent e) {}
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) { panel.keyTypedOnImage(e.getKeyChar()); }
}
