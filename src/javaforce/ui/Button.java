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
    setFocusable(true);
  }
  public Dimension getMinSize() {
    FontMetrics metrics = getFont().getMetrics(getText());
    return new Dimension(metrics.getWidth() + 8, metrics.getHeight() + 8);
  }
  protected void renderText(Image image, int dx, int dy) {
    image.drawText(pos.x + dx, pos.y + dy - getFont().getMaxAscent(), getText());
  }
  public void render(Image image) {
    int x = pos.x;
    int y = pos.y;
    int w = size.width;
    int h = size.height;
    if (w == 0 || h == 0) return;
    if (isEnabled()) {
      image.setForeColor(getForeColor());
    } else {
      image.setForeColor(getDisabledColor());
    }
    image.setLineStyle(LineStyle.SOLID);
    image.drawBox(x, y, w, h);
    if (isFocused()) {
      image.setLineStyle(LineStyle.DASH);
      image.drawBox(x + 1, y + 1, w - 2, h - 2);
      image.setLineStyle(LineStyle.SOLID);
    }
    renderText(image, 4, 4);
  }
  public String toString() {
    return "Button:" + getText();
  }
  protected void doAction() {
    if (!isEnabled()) return;
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
