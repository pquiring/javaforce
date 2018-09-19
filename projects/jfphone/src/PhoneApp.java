import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javaforce.*;

/** Entry point for jfPhone application. */

public class PhoneApp extends JFrame implements WindowListener, WindowController {
  public static void main(String args[]) {
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new PhoneApp().setVisible(true);
      }
    });
  }

  private PhonePanel panel;

  private PhoneApp() {
    if (!BasePhone.lockFile()) {
      JFAWT.showError("Error", "Another instance of jfPhone is already running!");
      System.exit(0);
    }
    panel = new PhonePanel(this, false);
    addWindowListener(this);
    setResizable(false);
    setContentPane(panel);
    pack();
    setTitle("jfPhone/" + PhonePanel.version);
    JFImage icon = new JFImage();
    icon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfphone.png"));
    setIconImage(icon.getImage());
    setPosition();
  }
//interface WindowListener
  public void windowOpened(WindowEvent e) { }
  public void windowClosing(WindowEvent e) {
    if (Settings.current.exitWhenClosed) {
      panel.unRegisterAll();
      Settings.saveSettings();
      BasePhone.unlockFile();
      System.exit(0);
    }
  }
  public void windowClosed(WindowEvent e) { }
  public void windowIconified(WindowEvent e) {
    if (Settings.current.hideWhenMinimized) {
      setVisible(false);
    }
  }
  public void windowDeiconified(WindowEvent e) { }
  public void windowActivated(WindowEvent e) {
    panel.active = true;
  }
  public void windowDeactivated(WindowEvent e) {
    panel.active = false;
  }
//interface WindowController
  public void setPanelSize() {
    pack();
  }
  public void setPanelVisible() {
    setVisible(true);
    setExtendedState(NORMAL);
  }
  public void setPanelAlwaysOnTop(boolean state) {
    setAlwaysOnTop(state);
  }
  public void startFlash() {}
  public void stopFlash() {}
  public void setPosition() {
    Dimension d = getPreferredSize();
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setLocation(s.width/2 - d.width/2, s.height/2 - d.height/2);
  }
}
