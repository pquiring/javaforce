/**
 * Created : Jun 28, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class MixTracks extends javax.swing.JDialog implements ActionListener {

  /**
   * Creates new form MixTracks
   */
  public MixTracks(java.awt.Frame parent, boolean modal, ProjectPanel project, ArrayList<TrackPanel> list) {
    super(parent, modal);
    initComponents();
    setPosition();
    this.trackList = list;
    this.project = project;
    for(int a=0;a<list.size();a++) {
      JToggleButton button = new JToggleButton("Track " + (a+1));
      button.addActionListener(this);
      button.setActionCommand("" + a);
      buttonList.add(button);
      tracks.add(button);
      list.get(a).unselectAll();
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

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tracks = new javax.swing.JPanel();
        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Select two Tracks to mix:");

        jScrollPane1.setViewportView(tracks);

        ok.setText("Ok");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 222, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ok)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ok)
                    .addComponent(cancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
    dispose();
  }//GEN-LAST:event_cancelActionPerformed

  private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
    mix();
  }//GEN-LAST:event_okActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton ok;
    private javax.swing.JPanel tracks;
    // End of variables declaration//GEN-END:variables

  public boolean accepted;
  public ArrayList<JToggleButton> buttonList = new ArrayList<JToggleButton>();
  public ProjectPanel project;
  public ArrayList<TrackPanel> trackList = new ArrayList<TrackPanel>();

  private void mix() {
    int cnt = 0;
    int idx[] = new int[2];
    idx[0] = -1;
    idx[1] = -1;
    for(int a=0;a<buttonList.size();a++) {
      if (buttonList.get(a).isSelected()) {
        if (idx[0] == -1) idx[0] = a;
        else if (idx[1] == -1) idx[1] = a;
        cnt++;
      }
    }
    if (cnt != 2) {
      JFAWT.showError("Error", "Must select exactly two tracks");
      return;
    }
    TrackPanel t1, t2, tmp;
    t1 = trackList.get(idx[0]);
    t2 = trackList.get(idx[1]);
    if (t1.bits != t2.bits) {
      JFAWT.showError("Error", "Samples bit sizes do not match");
      return;
    }
    if (t1.channels != t2.channels) {
      JFAWT.showError("Error", "Number of channels do not match");
      return;
    }
    if (t1.rate != t2.rate) {
      if (!JFAWT.showConfirm("Warning", "Frequecy rates do not match, continue anyways?")) {
        return;
      }
    }
    if (t1.totalLength < t2.totalLength) {
      tmp = t1;
      t1 = t2;
      t2 = tmp;
    }
    project.deleteUndo();
    //mix t2 into t1 and delete t2
    long length = t2.totalLength;
    long offset = 0;
    while (length > 0) {
      int toRead = TrackPanel.maxChunkSize;
      if (toRead > length) toRead = (int)length;
      for(int ch=0;ch<t1.channels;ch++) {
        byte samples1[] = t1.getSamples(offset, toRead, ch);
        byte samples2[] = t2.getSamples(offset, toRead, ch);
        byte samplesOut[] = null;
        switch (t1.bits) {
          case 16:
            short samples16_1[] = LE.byteArray2shortArray(samples1, null);
            short samples16_2[] = LE.byteArray2shortArray(samples2, null);
            for(int a=0;a<samples16_1.length;a++) {
              samples16_1[a] += samples16_2[a];
            }
            samplesOut = LE.shortArray2byteArray(samples16_1, null);
            break;
          case 32:
            int samples32_1[] = LE.byteArray2intArray(samples1, null);
            int samples32_2[] = LE.byteArray2intArray(samples2, null);
            for(int a=0;a<samples32_1.length;a++) {
              samples32_1[a] += samples32_2[a];
            }
            samplesOut = LE.intArray2byteArray(samples32_1, null);
            break;
        }
        t1.setSamples(offset, samplesOut, ch);
      }
      offset += toRead;
      length -= toRead;
    }
    t2.selectTrack(true);
    project.deleteTrack();
    dispose();
  }
  private void setPosition() {
    Dimension d = getSize();
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setLocation(s.width/2 - d.width/2, s.height/2 - d.height/2);
  }

  public void actionPerformed(ActionEvent ae) {
    JToggleButton button = (JToggleButton)ae.getSource();
    int lidx = JF.atoi(button.getActionCommand());
    if (button.isSelected())
      trackList.get(lidx).selectTrack(false);
    else
      trackList.get(lidx).unselectAll();
  }
}
