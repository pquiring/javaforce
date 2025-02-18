package javaforce.service;

/**
 *
 * @author pquiring
 *
 * Created : Nov 16, 2013
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jbus.*;

public class MQTTApp extends javax.swing.JFrame {

  /**
   * Creates new form MQTTApp
   */
  public MQTTApp() {
    initComponents();
    //create tray icon to open app
    JFImage img = new JFImage();
    img.loadPNG(this.getClass().getResourceAsStream("/javaforce/icons/mqtt.png"));
    new Thread() {
      public void run() {
        Random r = new Random();
        busClient = new JBusClient(MQTTServer.busPack + ".client" + r.nextInt(), new JBusMethods());
        busClient.setPort(MQTTServer.getBusPort());
        busClient.start();
        busClient.call(MQTTServer.busPack, "getConfig", "\"" + busClient.pack + "\"");
      }
    }.start();
    JFAWT.centerWindow(this);
  }

  public void writeConfig() {
    busClient.call(MQTTServer.busPack, "setConfig", busClient.quote(busClient.encodeString(config.getText())));
  }

  public void restart() {
    busClient.call(MQTTServer.busPack, "restart", "");
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
    gen_keys = new javax.swing.JButton();
    viewLog = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("MQTT Server");

    save.setText("Save");
    save.setEnabled(false);
    save.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveActionPerformed(evt);
      }
    });

    config.setColumns(20);
    config.setRows(5);
    config.setText(" [ loading ... ]");
    config.setEnabled(false);
    jScrollPane1.setViewportView(config);

    jLabel1.setText("MQTT Configuration:");

    gen_keys.setText("Generate SSL Key");
    gen_keys.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        gen_keysActionPerformed(evt);
      }
    });

    viewLog.setText("View Log");
    viewLog.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        viewLogActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(jScrollPane1)
          .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
            .addComponent(viewLog)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(gen_keys)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 307, Short.MAX_VALUE)
            .addComponent(save))
          .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
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
          .addComponent(gen_keys))
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

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    for(String arg : args) {
      switch (arg) {
        case "createKeys":
          MQTTServer.createKeys();
          System.exit(0);
          break;
      }
    }
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new MQTTApp().setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextArea config;
  private javax.swing.JButton gen_keys;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JButton save;
  private javax.swing.JButton viewLog;
  // End of variables declaration//GEN-END:variables

  public ViewLog viewer;

  public void showViewLog() {
    if (viewer == null || viewer.isClosed) {
      viewer = new ViewLog(MQTTServer.getLogFile());
      viewer.setTitle("MQTT Log");
    }
    viewer.setVisible(true);
  }

  private void genKeys() {
    busClient.call(MQTTServer.busPack, "genKeys", "\"" + busClient.pack + "\"");
  }

  public JBusClient busClient;

  public class JBusMethods {
    public void getConfig(String cfg) {
      final String _cfg = cfg;
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          config.setText(JBusClient.decodeString(_cfg));
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
}
