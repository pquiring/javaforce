import java.awt.*;
import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;

/**
 *
 * @author pquiring
 */
public class VideoApp extends javax.swing.JFrame {

  public static String version = "0.24";

  /**
   * Creates new form VideoApp
   */
  public VideoApp() {
    initComponents();
    mainPanel = new MainPanel();
    setContentPane(mainPanel);
    Menu.create(this, mainPanel);
    JFImage icon = new JFImage();
    icon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfvideo.png"));
    setIconImage(icon.getImage());
    setPosition();
    setTitle("jfVideo Creator/" + version);
    new Thread() {
      public void run() {
        checkVersion();
      }
    }.start();
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
    setTitle("jfVideo Creator");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 900, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 698, Short.MAX_VALUE)
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
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        VideoApp app = new VideoApp();
        app.setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  public static boolean inDialog = false;
  public MainPanel mainPanel;

  /** Checks for an online update on startup. */

  public void checkVersion() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        new URI("http://jfvideo.sourceforge.net/version.php").toURL().openStream()));
      String line = reader.readLine();
      if (line.equals(version)) {JFLog.log("version is up-to-date"); return;}
      JFLog.log("newer version is available : " + line);
      JFAWT.showMessage("Info",
        "A newer version of jfVideo is available! (v" + line + ")\r\nPlease goto http://jfvideo.sourceforge.net to download it"
      );
    } catch (Exception e) {
      JFLog.log("err:unable to check for version update");
      JFLog.log(e);
    }
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
