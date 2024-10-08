/**
 * Created : Mar 25, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;
import javaforce.voip.*;

public class MediaApp extends javax.swing.JFrame {

  public static String version = "0.22";

  /**
   * Creates new form MediaApp
   */
  public MediaApp() {
    RTSPURL.register();
    initComponents();
    JFImage icon = new JFImage();
    icon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfmedia.png"));
    setIconImage(icon.getImage());
    frame = this;
    panel = new MainPanel();
    setContentPane(panel);
    setPosition();
    if (args.length > 0) {
      String arg = args[0];
      JFLog.log("arg[]=" + arg);
      if (arg.startsWith("rtsp://")) {
        try {
          panel.play(new URI(arg).toURL());
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        File file = new File(args[0]);
        if (file.exists()) {
          panel.play(file);
        }
      }
    }
    setTitle("jfMedia/" + version);
    setExtendedState(Frame.MAXIMIZED_BOTH);
    fullscreen = true;
    JFAWT.assignHotKey(getRootPane(), new Runnable() {
      public void run() {
        showHelp();
      }
    }, KeyEvent.VK_F1);
    JFAWT.assignHotKey(getRootPane(), new Runnable() {
      public void run() {
        toggleFullscreen();
      }
    }, KeyEvent.VK_F10);
  }

  public void showHelp() {
    JFAWT.showMessage("Help",
      "jfMedia Player/" + version + "\n\n"
      + "F1 = Help\n"
      + "F10 = Full Screen\n"
    );
  }

  public void toggleFullscreen() {
    if (fullScreen) {
      setExtendedState(Frame.NORMAL);
    } else {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    }
    fullScreen = !fullScreen;
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
   * Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("jMedia");
    setPreferredSize(new java.awt.Dimension(1280, 720));

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 952, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 801, Short.MAX_VALUE)
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    MediaCoder.init();
    MediaApp.args = args;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new MediaApp().setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  public static MediaApp frame;
  public static String args[];
  public static MainPanel panel;
  public boolean fullscreen;

  private void setPosition() {
    Dimension d = getSize();
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    if ((d.width > s.width) || (d.height > s.height)) {
      if (d.width > s.width) {
        d.width = s.width;
      }
      if (d.height > s.height) {
        d.height = s.height;
      }
      setSize(d);
    }
    setLocation(s.width / 2 - d.width / 2, s.height / 2 - d.height / 2);
  }

  public boolean fullScreen = false;

  public void toggleFullScreen() {
    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    if (fullScreen) {
      gd.setFullScreenWindow(null);
    } else {
      gd.setFullScreenWindow(this);
    }
    fullScreen = !fullScreen;
  }

  public boolean isFullScreen() {
    return fullScreen;
  }
}
