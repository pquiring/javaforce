package javaforce.ui;

/** ListItem
 *
 * @author pquiring
 */

import javaforce.*;

public class ListItem extends Label {
  private boolean selected;
  private ActionListener action;

  public ListItem(String text) {
    super(text);
  }

  public void render(Image image) {
    int x = pos.x;
    int y = pos.y;
    int w = size.width;
    int h = size.height;
    if (w == 0 || h == 0) return;
    if (isSelected()) {
      image.setForeColor(getSelectedColor());
      image.fill(x, y, w, h);
    }
    super.render(image);
  }

  protected void doAction() {
    if (!isEnabled()) return;
    if (action != null) {
      action.actionPerformed(this);
    }
  }

  public void mouseDown(int button) {
    if (button == MouseButton.LEFT) {
      selected = !selected;
      doAction();
    }
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean state) {
    selected = state;
  }

  public void setActionListener(ActionListener action) {
    this.action = action;
  }
}
