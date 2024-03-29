package jfrepo;

/**
 * Created : Feb 28, 2012
 *
 * @author pquiring
 */

import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class RepoApp extends javax.swing.JFrame {

  public static String version = "0.2";

  /**
   * Creates new form RepoApp
   */
  public RepoApp() {
    JFLog.init(JF.getUserPath() + "/.jfrepo.log", true);
    initComponents();
    JPanel panel = new MainPanel();
    setContentPane(panel);
    setTitle("jfrepo/" + version);
    pack();
    JFAWT.centerWindow(this);
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
    setTitle("jfrepo");
    setName("frame"); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 250, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 182, Short.MAX_VALUE)
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    RepoApp.args = args;
    /*
     * Create and display the form
     */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new RepoApp().setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  public static String args[];
}
