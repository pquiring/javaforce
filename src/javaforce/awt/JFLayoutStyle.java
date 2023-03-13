package javaforce.awt;

import java.awt.Container;
import javax.swing.JComponent;

/**
 * Provides a uniform layout style.
 *
 * To use in ctor:
 *
 * public myComponent {
 *   initComponents();
 *   GroupLayout layout = (GroupLayout)getLayout();
 *   layout.setLayoutStyle(new JFLayoutStyle(1));
 * }
 *
 * @author pquiring
 */

public class JFLayoutStyle extends javax.swing.LayoutStyle {
  private int size;

  public JFLayoutStyle(int size) {
    this.size = size;
  }

  public int getPreferredGap(JComponent component1, JComponent component2, ComponentPlacement type, int position, Container parent) {
    return size;
  }

  public int getContainerGap(JComponent component, int position, Container parent) {
    return size;
  }
}
