package javaforce.awt;

/*
 * ReplaceDialog.java
 *
 * Created on July 29, 2007, 4:04 PM
 */

import java.awt.event.KeyEvent;

/**
 * Opens a replace text dialog that uses the ReplaceEvent interface for event
 * handling.
 *
 * @author Peter Quiring
 */
public class ReplaceDialog extends javax.swing.JDialog {

  /**
   * Creates new form ReplaceDialog
   */
  private ReplaceDialog(java.awt.Frame parent, boolean modal) {
    super(parent, modal);
    initComponents();
    setTitle("Replace");
    setComponentOrientation(((parent == null) ? javax.swing.JOptionPane.getRootFrame() : parent).getComponentOrientation());
    if (parent != null) {
      setLocationRelativeTo(parent);
    }
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    findText = new javax.swing.JTextField();
    replaceText = new javax.swing.JTextField();
    cbWhole = new javax.swing.JCheckBox();
    cbCase = new javax.swing.JCheckBox();
    findReplace = new javax.swing.JButton();
    replaceAll = new javax.swing.JButton();
    cancel = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    jLabel1.setText("Find What:");

    jLabel2.setText("Replace With:");

    findText.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    replaceText.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    cbWhole.setMnemonic('w');
    cbWhole.setText("Match whole word only");
    cbWhole.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    cbWhole.setMargin(new java.awt.Insets(0, 0, 0, 0));
    cbWhole.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    cbCase.setMnemonic('c');
    cbCase.setText("Match case sensitive");
    cbCase.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    cbCase.setMargin(new java.awt.Insets(0, 0, 0, 0));
    cbCase.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    findReplace.setMnemonic('f');
    findReplace.setText("Find");
    findReplace.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        findReplaceActionPerformed(evt);
      }
    });
    findReplace.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    replaceAll.setMnemonic('a');
    replaceAll.setText("Replace All");
    replaceAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        replaceAllActionPerformed(evt);
      }
    });
    replaceAll.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    cancel.setText("Cancel");
    cancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelActionPerformed(evt);
      }
    });
    cancel.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        anykey(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel2)
              .addComponent(jLabel1))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(findText, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
              .addComponent(replaceText, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)))
          .addComponent(cbWhole)
          .addComponent(cbCase)
          .addGroup(layout.createSequentialGroup()
            .addComponent(findReplace)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(replaceAll)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cancel)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(findText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(replaceText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cbWhole)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cbCase)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(findReplace)
          .addComponent(replaceAll)
          .addComponent(cancel))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void anykey(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_anykey
      int keyCode = evt.getKeyCode();
      int mods = evt.getModifiers();
      if (keyCode == KeyEvent.VK_ESCAPE && mods == 0) {
        cancelActionPerformed(null);
      }
      if (keyCode == KeyEvent.VK_ENTER && mods == 0) {
        findReplaceActionPerformed(null);
      }
      if (keyCode == KeyEvent.VK_A && mods == KeyEvent.ALT_MASK) {
        replaceAllActionPerformed(null);
      }
    }//GEN-LAST:event_anykey

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
      this.dispose();
    }//GEN-LAST:event_cancelActionPerformed

    private void replaceAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllActionPerformed
      re.replaceAllEvent(this);
      this.dispose();
    }//GEN-LAST:event_replaceAllActionPerformed

    private void findReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findReplaceActionPerformed
      if (!found) {
        found = re.findEvent(this);
        if (found) {
          findReplace.setText("Replace");
        }
      } else {
        re.replaceEvent(this);
        found = false;
        findReplace.setText("Find");
      }
    }//GEN-LAST:event_findReplaceActionPerformed
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton cancel;
  private javax.swing.JCheckBox cbCase;
  private javax.swing.JCheckBox cbWhole;
  private javax.swing.JButton findReplace;
  private javax.swing.JTextField findText;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JButton replaceAll;
  private javax.swing.JTextField replaceText;
  // End of variables declaration//GEN-END:variables
  private ReplaceEvent re;
  private boolean found = false;

  public String getFindText() {
    return findText.getText();
  }

  public String getReplaceText() {
    return replaceText.getText();
  }

  public boolean getWhole() {
    return cbWhole.isSelected();
  }

  public boolean getCase() {
    return cbCase.isSelected();
  }

  public static void showReplaceDialog(java.awt.Frame parent, boolean modular, String findText, String replaceText, boolean bWhole, boolean bCase, ReplaceEvent re) {
    ReplaceDialog dialog = new ReplaceDialog(parent, modular);
    dialog.re = re;
    dialog.findText.setText(findText);
    dialog.replaceText.setText(replaceText);
    dialog.cbWhole.setSelected(bWhole);
    dialog.cbCase.setSelected(bCase);
    dialog.findText.selectAll();
    dialog.setVisible(true);
  }
}
