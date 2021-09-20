/** Paint application. */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javaforce.*;
import javaforce.awt.*;

public class PaintApp extends JFrame implements WindowListener {
  public static String args[];
  public static void main(String args[]) {
    PaintApp.args = args;
    if (JF.isMac()) {
      JFAWT.setMetalLAF();
    }
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new PaintApp();
      }
    });
  }

  private MainPanel panel;
  private Insets insets;

  private PaintApp() {
    panel = new MainPanel(this, null);
    addWindowListener(this);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setContentPane(panel);
    setSize(800, 600);  //TODO : settings
    setPosition();
    setTitle("jfPaint/" + MainPanel.version);
    setVisible(true);
    revalidate();
    JFImage icon = new JFImage();
    icon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfpaint.png"));
    setIconImage(icon.getImage());
    panel.loadFiles(args);
    Menu.create(this, panel);
  }
//interface WindowListener
  public void windowOpened(WindowEvent e) { }
  public void windowClosing(WindowEvent e) {
    if (panel.closeAll()) {
      System.exit(0);
    }
  }
  public void windowClosed(WindowEvent e) { }
  public void windowIconified(WindowEvent e) { }
  public void windowDeiconified(WindowEvent e) { }
  public void windowActivated(WindowEvent e) {
    panel.active = true;
  }
  public void windowDeactivated(WindowEvent e) {
    panel.active = false;
  }
  private void setPosition() {
    Dimension d = getSize();
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    if ((d.width > s.width) || (d.height > s.height)) {
      if (d.width > s.width) d.width = s.width;
      if (d.height > s.height) d.height = s.height;
      setSize(d);
    }
    setLocation(s.width/2 - d.width/2, s.height/2 - d.height/2);
  }
}
