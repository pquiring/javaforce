/**
 *
 * @author pquiring
 */

import java.awt.*;
import javax.swing.*;

import javaforce.*;

public class PanelTools {
  public static ThreeDeeApp frame;

  public static final int VERTICAL = JSplitPane.VERTICAL_SPLIT;
  public static final int HORIZONTAL = JSplitPane.HORIZONTAL_SPLIT;

  public static JSplitPane createSplit(int orientation) {
    JSplitPane split = new JSplitPane(orientation);
    split.setDividerSize(3);
    return split;
  }

  public static String otherSide(String side) {
    if (side.equals("left")) return "right";
    return null;
  }

  public static void swapPanels(JPanel target, JPanel dest) {
    JSplitPane parent = (JSplitPane)target.getClientProperty("panel");
    String side = (String)target.getClientProperty("side");
    String other = otherSide(side);
    parent.add(target, other);
    parent.add(dest, side);
  }

  public static void setPanel(JSplitPane split, JPanel panel, String side) {
    split.add(panel, side);
    panel.putClientProperty("panel", panel);
    panel.putClientProperty("side", side);
  }

  public static void splitPanel(JPanel panel, int orientation) {
    JSplitPane parent = (JSplitPane)panel.getClientProperty("panel");
    String side = (String)panel.getClientProperty("side");
    JSplitPane child = new JSplitPane(orientation);
    parent.add(child, side);
    switch (orientation) {
      case VERTICAL: child.add(panel, "left"); break;
      case HORIZONTAL: child.add(panel, "top"); break;
    }
  }

  public static JLabel createPanelSelector() {
    JLabel lbl = new JLabel("X");

    return lbl;
  }

  public static void mergePanel(JPanel panel) {

  }

  public static void minimize(JPanel panel) {
    Insets zero = new Insets(0,0,0,0);
    Font font = JF.getMonospacedFont(0, 10);
    int cnt = panel.getComponentCount();
    for(int a=0;a<cnt;a++) {
      Component c = panel.getComponent(a);
      if (c instanceof JButton) {
        JButton b = (JButton)c;
        b.setMargin(zero);
        b.setFont(font);
      }
    }
    GroupLayout layout = (GroupLayout)panel.getLayout();
    layout.setLayoutStyle(new JFLayoutStyle(2));
  }
}
