package viewer;

/** MediaDownload
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;

public class MediaDownload extends javax.swing.JDialog implements MediaIO {

  /**
   * Creates new form MediaDownload
   */
  public MediaDownload(java.awt.Frame parent, boolean modal, Viewer viewer, String filename) {
    super(parent, modal);
    initComponents();
    this.viewer = viewer;
    try {
      raf = new RandomAccessFile(filename, "rw");
    } catch (Exception e) {
      JFLog.log(e);
    }
    JFAWT.centerWindow(this);
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
   * Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    progress = new javax.swing.JProgressBar();
    close = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Download Media");

    progress.setIndeterminate(true);

    close.setText("Cancel");
    close.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        closeActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(close)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addGap(14, 14, 14)
        .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(close)
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
    abort();
  }//GEN-LAST:event_closeActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton close;
  private javax.swing.JProgressBar progress;
  // End of variables declaration//GEN-END:variables

  private RandomAccessFile raf;
  private Viewer viewer;
  private boolean complete;
  public int read(MediaCoder coder, byte data[]) {
    try {
      return raf.read(data);
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }
  public int write(MediaCoder coder, byte data[]) {
    try {
      raf.write(data);
      return data.length;
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }
  public long seek(MediaCoder coder, long pos, int how) {
    try {
      switch (how) {
        case MediaCoder.SEEK_SET: raf.seek(pos); break;
        case MediaCoder.SEEK_CUR: raf.seek(raf.getFilePointer() + pos); break;
        case MediaCoder.SEEK_END: raf.seek(raf.length() + pos); break;
      }
      return raf.getFilePointer();
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  private void abort() {
    if (!complete) {
      viewer.stopDownload();
    }
    dispose();
  }
}
