/** Edit Document Info
 *
 * @author pquiring
 */

import java.awt.event.*;

import javaforce.*;
import javaforce.awt.*;

public class EditDocInfo extends javax.swing.JDialog {

  /**
   * Creates new form EditDocInfo
   */
  public EditDocInfo(java.awt.Frame parent, boolean modal, String file, int lines, int x, int y, boolean unix, String encoding) {
    super(parent, modal);
    initComponents();
    this.file.setText(file);
    this.lines.setText(Integer.toString(lines));
    this.x.setText(String.format("%d",x));
    this.y.setText(String.format("%d",y));
    this.eol.setSelectedIndex(unix ? 1 : 0);
    switch (encoding) {
      case Encodings.utf8: this.encoding.setSelectedIndex(0); break;
      case Encodings.utf16be: this.encoding.setSelectedIndex(1); break;
      case Encodings.utf16le: this.encoding.setSelectedIndex(2); break;
    }
    JFAWT.assignHotKey(this, ok, KeyEvent.VK_ENTER);
    JFAWT.assignHotKey(this, cancel, KeyEvent.VK_ESCAPE);
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

    jLabel1 = new javax.swing.JLabel();
    file = new javax.swing.JTextField();
    jLabel2 = new javax.swing.JLabel();
    lines = new javax.swing.JTextField();
    jLabel3 = new javax.swing.JLabel();
    x = new javax.swing.JTextField();
    jLabel4 = new javax.swing.JLabel();
    eol = new javax.swing.JComboBox<>();
    ok = new javax.swing.JButton();
    cancel = new javax.swing.JButton();
    jLabel5 = new javax.swing.JLabel();
    jLabel6 = new javax.swing.JLabel();
    y = new javax.swing.JTextField();
    jLabel7 = new javax.swing.JLabel();
    encoding = new javax.swing.JComboBox<>();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Document Info");

    jLabel1.setText("File:");

    file.setEditable(false);

    jLabel2.setText("Lines:");

    lines.setEditable(false);

    jLabel3.setText("Position:");

    x.setEditable(false);
    x.setMinimumSize(new java.awt.Dimension(100, 22));
    x.setPreferredSize(new java.awt.Dimension(100, 22));

    jLabel4.setText("EOL:");

    eol.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "CRLF", "LF" }));

    ok.setText("OK");
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

    jLabel5.setText("X:");

    jLabel6.setText("Y:");

    y.setEditable(false);
    y.setMinimumSize(new java.awt.Dimension(100, 22));
    y.setPreferredSize(new java.awt.Dimension(100, 22));

    jLabel7.setText("Encoding:");

    encoding.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "UTF-8", "UTF-16BE", "UTF-16LE" }));

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(file))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel2)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lines))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel4)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(eol, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 262, Short.MAX_VALUE)
            .addComponent(cancel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(ok))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel3)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel5)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(x, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel6)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel7)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(encoding, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(file, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(lines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3)
          .addComponent(x, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel5)
          .addComponent(jLabel6)
          .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(eol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel4))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel7)
          .addComponent(encoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
    accepted = true;
    dispose();
  }//GEN-LAST:event_okActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton cancel;
  private javax.swing.JComboBox<String> encoding;
  private javax.swing.JComboBox<String> eol;
  private javax.swing.JTextField file;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JTextField lines;
  private javax.swing.JButton ok;
  private javax.swing.JTextField x;
  private javax.swing.JTextField y;
  // End of variables declaration//GEN-END:variables

  public boolean accepted;

  public boolean getUnix() {
    return eol.getSelectedIndex() == 1;
  }

  public String getEncoding() {
    switch (encoding.getSelectedIndex()) {
      case 0: return Encodings.utf8;
      case 1: return Encodings.utf16be;
      case 2: return Encodings.utf16le;
    }
    return null;
  }
}
