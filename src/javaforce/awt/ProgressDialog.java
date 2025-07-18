package javaforce.awt;

/**
 * Created : Mar 12, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.KeyEvent;

import javaforce.*;

public class ProgressDialog extends javax.swing.JDialog implements JFTaskListener {

  /**
   * Creates new form ProgressDialog
   */
  public ProgressDialog(java.awt.Frame parent, boolean modal, JFTask task) {
    super(parent, modal);
    initComponents();
    setPosition();
    this.task = task;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progress = new javax.swing.JProgressBar();
        close = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        msg = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        close.setText("Close");
        close.setEnabled(false);
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        msg.setText("...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(close))
                    .addComponent(msg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(msg)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(close)
                    .addComponent(cancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
    String results = (String)task.getProperty("results");
    if (results == null) {
      task.abort();
    } else {
      ViewLog log = new ViewLog(results);
      log.setVisible(true);
    }
  }//GEN-LAST:event_cancelActionPerformed

  private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
    dispose();
  }//GEN-LAST:event_closeActionPerformed

  private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    task.abort();
  }//GEN-LAST:event_formWindowClosing
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JButton close;
    private javax.swing.JLabel msg;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables
  private JFTask task;
  private boolean indeterminate = true;
  private boolean taskStarted = false;
  private boolean autoClose = false;

  public void done() {
    if (autoClose) {
      dispose();
      return;
    }
    if (task.getProperty("results") == null) {
      cancel.setEnabled(false);
    } else {
      cancel.setText("Details");
    }
    close.setEnabled(true);
    JFAWT.assignHotKey(this, close, KeyEvent.VK_ENTER);
  }

  /**
   * Sets Progress Value (0-100) (-1 = indeterminate)
   */
  public void setProgress(int value) {
    if (value == -1) {
      progress.setIndeterminate(true);
      indeterminate = true;
    } else {
      if (indeterminate) {
        progress.setIndeterminate(false);
        indeterminate = false;
      }
      progress.setValue(value);
    }
  }

  public void setLabel(String txt) {
    msg.setText(txt);
  }

  public boolean getStatus() {
    return task.getStatus();
  }

  private void setPosition() {
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension d = getSize();
    setLocation(s.width / 2 - d.width / 2, s.height / 2 - (d.height / 2));
  }

  public void setVisible(boolean state) {
    if ((state) && (!taskStarted)) {
      task.start(this);
      taskStarted = true;
    }
    super.setVisible(state);
  }

  public void setAutoClose(boolean state) {
    autoClose = state;
  }
}
