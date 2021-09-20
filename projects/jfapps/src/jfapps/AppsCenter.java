package jfapps;

/**
 * Created : Mar 2, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import javaforce.*;
import javaforce.awt.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AppsCenter extends javax.swing.JFrame {

  /**
   * Creates new form AppsCenter
   */
  public AppsCenter() {
    JFLog.init(JF.getUserPath() + "/.jfapps.log", true);
    initComponents();
    JF.initHttps();
    setContentPane(new MainPanel());
    setPosition();
    if ((args.length > 0) && (args[0].length() > 0)) {
      installPackage(args[0]);
    }
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
        setTitle("Apps Center");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 732, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 543, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    AppsCenter.args = args;
    /*
     * Create and display the form
     */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new AppsCenter().setVisible(true);
      }
    });
  }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

  private static String args[];

  private static String cmd[];

  private void installPackage(String file) {
    String tool = null;
    if (file.toLowerCase().endsWith(".deb")) {
      tool = "dpkg";
    } else if (file.toLowerCase().endsWith(".rpm")) {
      tool = "rpm";
    }
    if (tool == null) {
      JFAWT.showError("Error", "That file is an unknown package type.");
      return;
    }
    if (!JFAWT.showConfirm("Confirm", "Are you sure you want to install '" + file + "' ?")) {
      return;
    }
    cmd = new String[] {"sudo", "-E", tool, "-i", file};
    JFTask task = new JFTask() {
      private int total = 25;  //guess
      private int current = 0;
      public boolean work() {
        ShellProcess sp = new ShellProcess();
        sp.removeEnvironmentVariable("TERM");
        sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
        sp.keepOutput(false);
        sp.addListener(this);
        this.setProgress(5);
        sp.run(cmd, false);
        return sp.getErrorLevel() == 0;
      }
      public void shellProcessOutput(String str) {
        current += str.split("\n").length - 1;
        this.setProgress(current * 100 / total);
      }
    };
    ProgressDialog dialog = new ProgressDialog(null, true, task);
    dialog.setVisible(true);
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
