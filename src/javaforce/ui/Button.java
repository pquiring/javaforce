package javaforce.ui;

/** Button
 *
 * @author pquiring
 */

import javaforce.*;

public class Button extends TextComponent {
  private ActionListener action;

  public Button(String text) {
    setText(text);
  }
  public Dimension getMinSize() {
    FontMetrics metrics = getFont().getMetrics(getText());
    return new Dimension(metrics.getWidth() + 6, metrics.getHeight() + 6);
  }
  public void render(Image image) {
    int x = pos.x;
    int y = pos.y;
    int w = size.width;
    int h = size.height;
    if (w == 0 || h == 0) return;
    image.drawBox(x + 1, y + 1, w, h);
    getFont().drawText(pos.x + 3, pos.y + 3 - getFont().getMaxAscent(), getText(), image, foreClr.getColor());
  }
  public String toString() {
    return "Button:" + getText();
  }
  private void doAction() {
    if (action != null) {
      action.actionPerformed(this);
    }
  }
  public void mouseUp(int button) {
    if (button == MouseButton.LEFT) {
      doAction();
    }
  }
  public void keyReleased(int key) {
    if (key == KeyCode.VK_SPACE) {
      doAction();
    }
  }
  public void setActionListner(ActionListener action) {
    this.action = action;
  }
}
