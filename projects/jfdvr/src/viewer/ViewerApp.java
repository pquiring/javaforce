package viewer;

/**
 * Created : Mar 25, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;
import javaforce.voip.*;

public class ViewerApp extends javax.swing.JFrame {

  /**
   * Creates new form MediaApp
   */
  public ViewerApp() {
    RTSPURL.register();
    initComponents();
    JFImage icon = new JFImage();
    icon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfdvr.png"));
    setIconImage(icon.getImage());
    cameraicon = new JFImage();
    cameraicon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("camera.png"));
    self = this;
    root = getContentPane();
    setPosition();
    setTitle("jfDVR Viewer/" + service.ConfigService.version + " (F1 = Help | F2 = Select View)");
    RTP.setPortRange(40000, 50000);
    if (args.length > 0 && args[0].startsWith("rtsp://")) {
      String arg = args[0];
      panel = new Viewer();
      try {
        Config.url = new URI(arg).toURL();
        panel.play(Config.url);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      SelectView dialog = new SelectView();
      if (args.length > 0) {
        dialog.setServer(args[0]);
      }
      setPanel(dialog);
    }
    setExtendedState(Frame.MAXIMIZED_BOTH);
    fullscreen = true;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("jfDVR Viewer");
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
    if (!MediaCoder.init()) {
      if (!MediaCoder.download()) return;
      if (!MediaCoder.init()) return;  //try again after download
    }
    ViewerApp.args = args;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new ViewerApp().setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  public static String[] args;
  public static Viewer panel;
  public static ViewerApp self;
  public static Container root;
  public static JFImage cameraicon;
  public boolean fullscreen;

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

  public static void stopViewer() {
    if (panel == null) return;
    panel.stop(true);
    panel = null;
  }

  public static void showHelp() {
    JFAWT.showMessage("Help",
      "jfDVR/" + service.ConfigService.version + "\n\n" +
      "F1 = Help\n" +
      "F2 = Select View\n" +
      "F5 = Refresh\n" +
      "F10 = Full Screen\n"
    );
  }

  public static void selectView() {
    stopViewer();
    SelectView view = new SelectView();
    setPanel(view);
    view.setVisible(true);
  }

  public void selectView(String type, String name) {
    try {
      Config.url = Config.changeURL("/" + type + "/" + name);
      panel = new Viewer();
      panel.play(Config.url);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void refresh() {
    panel.refresh();
  }

  public static void toggleFullscreen() {
    if (self.fullScreen) {
      self.setExtendedState(Frame.NORMAL);
    } else {
      self.setExtendedState(Frame.MAXIMIZED_BOTH);
    }
    self.fullScreen = !self.fullScreen;
  }

  public static void setPanel(JPanel panel) {
    JFLog.log("setPanel:" + panel);
    if (panel == null) {
      self.setContentPane(root);
    } else {
      self.setContentPane(panel);
    }
    JFAWT.assignHotKey(panel.getRootPane(), new Runnable() {public void run() {showHelp();}}, KeyEvent.VK_F1);
    JFAWT.assignHotKey(panel.getRootPane(), new Runnable() {public void run() {selectView();}}, KeyEvent.VK_F2);
    JFAWT.assignHotKey(panel.getRootPane(), new Runnable() {public void run() {refresh();}}, KeyEvent.VK_F5);
    JFAWT.assignHotKey(panel.getRootPane(), new Runnable() {public void run() {toggleFullscreen();}}, KeyEvent.VK_F10);
    panel.revalidate();
  }
}