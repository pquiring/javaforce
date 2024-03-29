package jffile;

/**
 *
 * @author pquiring
 */

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.jbus.*;

public class Drives extends javax.swing.JDialog {

  /**
   * Creates new form Drives
   */
  public Drives(java.awt.Frame parent, boolean modal, JBusClient jbusClient) {
    super(parent, modal);
    initComponents();
    setPosition();
    listDrives();
    this.jbusClient = jbusClient;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    list = new javax.swing.JList();
    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    mount = new javax.swing.JButton();
    mounted = new javax.swing.JTextField();
    type = new javax.swing.JTextField();
    jButton3 = new javax.swing.JButton();
    umount = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Drives");
    setResizable(false);

    list.setModel(model);
    list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
        listValueChanged(evt);
      }
    });
    jScrollPane1.setViewportView(list);

    jLabel1.setText("Type:");

    jLabel2.setText("Mounted:");

    mount.setText("Mount");
    mount.setEnabled(false);
    mount.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mountActionPerformed(evt);
      }
    });

    mounted.setEditable(false);

    type.setEditable(false);

    jButton3.setText("Close");
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });

    umount.setText("UnMount");
    umount.setEnabled(false);
    umount.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        umountActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel2)
              .addComponent(jLabel1))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(mounted)
              .addComponent(type)))
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(jButton3))
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(mount)
              .addComponent(umount))
            .addGap(0, 177, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel1)
              .addComponent(type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel2)
              .addComponent(mounted, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(mount)
            .addGap(18, 18, 18)
            .addComponent(umount)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 155, Short.MAX_VALUE)
            .addComponent(jButton3))
          .addComponent(jScrollPane1))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listValueChanged
    showInfo();
  }//GEN-LAST:event_listValueChanged

  private void mountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mountActionPerformed
    mount();
  }//GEN-LAST:event_mountActionPerformed

  private void umountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umountActionPerformed
    umount();
  }//GEN-LAST:event_umountActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    dispose();
  }//GEN-LAST:event_jButton3ActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton3;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JList list;
  private javax.swing.JButton mount;
  private javax.swing.JTextField mounted;
  private javax.swing.JTextField type;
  private javax.swing.JButton umount;
  // End of variables declaration//GEN-END:variables

  private DefaultListModel<String> model = new DefaultListModel<>();
  private ArrayList<String> devList = new ArrayList<String>();
  private JBusClient jbusClient;

  private void addDrive(String name) {
    char lastChar = name.charAt(name.length() - 1);
    if ((lastChar < '0') || (lastChar > '9')) return;
    model.addElement("Storage Unit:" + name);
    devList.add(name);
  }

  private void addOptical(String name) {
    model.addElement("Optical Unit:" + name);
    devList.add(name);
  }

  private void listDrives() {
    model.removeAllElements();
    devList.clear();
    File dev = new File("/dev");
    File devs[] = dev.listFiles();
    for(int a=0;a<devs.length;a++) {
      String name = devs[a].getAbsolutePath();
      if (name.startsWith("/dev/sd")) addDrive(name);
      if (name.startsWith("/dev/sr")) addOptical(name);
    }
  }

  private void showInfo() {
    int idx = list.getSelectedIndex();
    if (idx == -1) return;
    String dev = devList.get(idx);
    type.setText("???");
    mounted.setText("???");
    mount.setEnabled(false);
    umount.setEnabled(false);
    jbusClient.call("org.jflinux.jfsystemmgr", "getStorageInfo", "\"" + jbusClient.pack + "\",\"" + dev + "\"");
  }

  public void storageInfo(String dev, String volName, String fsType, String mountPt) {
    type.setText(fsType);
    mounted.setText(mountPt);
    if (!fsType.equals("unknown") && (!mountPt.equals("/"))) {
      mount.setEnabled(true);
      umount.setEnabled(true);
    }
  }

  private void mount() {
    int idx = list.getSelectedIndex();
    if (idx == -1) return;
    String dev = devList.get(idx);
    jbusClient.call("org.jflinux.jfsystemmgr", "mount", "\"" + dev + "\"");
  }

  private void umount() {
    int idx = list.getSelectedIndex();
    if (idx == -1) return;
    String dev = devList.get(idx);
    jbusClient.call("org.jflinux.jfsystemmgr", "umount", "\"" + dev + "\"");
  }

  public void rescan() {
    listDrives();
    mount.setEnabled(false);
    umount.setEnabled(false);
    type.setText("");
    mounted.setText("");
  }

  private void setPosition() {
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension d = getSize();
    setLocation(s.width/2 - d.width/2, s.height/2 - (d.height/2));
  }
}
