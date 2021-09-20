package javaforce.service;

/** SOCKS Server Config App
 *
 * @author pquiring
 *
 * Created : Sept 14, 2020
 */

import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jbus.*;

public class SOCKSApp extends javax.swing.JFrame {

  /**
   * Creates new form SOCKSApp
   */
  public SOCKSApp() {
    initComponents();
    JFImage img = new JFImage();
    img.loadPNG(this.getClass().getResourceAsStream("/javaforce/icons/socks.png"));
    setIconImage(img.getImage());
    new Thread() {
      public void run() {
        Random r = new Random();
        busClient = new JBusClient(SOCKS.busPack + ".client" + r.nextInt(), new JBusMethods());
        busClient.setPort(SOCKS.getBusPort());
        busClient.start();
        busClient.call(SOCKS.busPack, "getConfig", "\"" + busClient.pack + "\"");
      }
    }.start();
    JFAWT.centerWindow(this);
  }

  public void writeConfig() {
    busClient.call(SOCKS.busPack, "setConfig", busClient.quote(busClient.encodeString(config.getText())));
  }

  public void restart() {
    busClient.call(SOCKS.busPack, "restart", "");
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    save = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    config = new javax.swing.JTextArea();
    jLabel1 = new javax.swing.JLabel();
    viewLog = new javax.swing.JButton();
    gen_keys = new javax.swing.JButton();
    help = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("SOCKS Server");

    save.setText("Save");
    save.setEnabled(false);
    save.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveActionPerformed(evt);
      }
    });

    config.setColumns(20);
    config.setRows(5);
    config.setText(" [ loading ... ]\n");
    config.setEnabled(false);
    jScrollPane1.setViewportView(config);

    jLabel1.setText("SOCKS Configuration:");

    viewLog.setText("View Log");
    viewLog.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        viewLogActionPerformed(evt);
      }
    });

    gen_keys.setText("Generate SSL Key");
    gen_keys.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        gen_keysActionPerformed(evt);
      }
    });

    help.setText("Help");
    help.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        helpActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(viewLog)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(gen_keys)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(help)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(save))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(save)
          .addComponent(viewLog)
          .addComponent(gen_keys)
          .addComponent(help))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
    writeConfig();
    restart();
    JFAWT.showMessage("Notice", "Settings saved!");
  }//GEN-LAST:event_saveActionPerformed

  private void viewLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewLogActionPerformed
    showViewLog();
  }//GEN-LAST:event_viewLogActionPerformed

  private void gen_keysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gen_keysActionPerformed
    genKeys();
  }//GEN-LAST:event_gen_keysActionPerformed

  private void helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpActionPerformed
    showHelp();
  }//GEN-LAST:event_helpActionPerformed

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new SOCKSApp().setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextArea config;
  private javax.swing.JButton gen_keys;
  private javax.swing.JButton help;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JButton save;
  private javax.swing.JButton viewLog;
  // End of variables declaration//GEN-END:variables

  public ViewLog viewer;

  public void showViewLog() {
    if (viewer == null || viewer.isClosed) {
      viewer = new ViewLog(SOCKS.getLogFile());
      viewer.setTitle("SOCKS Log");
    }
    viewer.setVisible(true);
    viewer.setExtendedState(JFrame.NORMAL);
  }

  public JBusClient busClient;

  public class JBusMethods {
    public void getConfig(String cfg) {
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          config.setText(JBusClient.decodeString(cfg));
          config.setEnabled(true);
          save.setEnabled(true);
        }
      });
    }
    public void getKeys(String status) {
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          if (status.equals("OK")) {
            JFAWT.showMessage("GenKeys", "OK");
          } else {
            JFAWT.showError("GenKeys", "Error");
          }
        }
      });
    }
  }

  private void genKeys() {
    busClient.call(SOCKS.busPack, "genKeys", "\"" + busClient.pack + "\"");
  }

  private void showHelp() {
    JFAWT.openURL("http://jfsocks.sf.net/help.html");
  }

}
