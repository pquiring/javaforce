package javaforce.ui;

/** Toggle Button
 *
 * @author pquiring
 */

import javaforce.*;

public class ToggleButton extends Button {
  private boolean selected;

  public ToggleButton(String text) {
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
    } else {
      image.setForeColor(getBackColor());
    }
    image.fill(x, y, w, h);
    super.render(image);
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean state) {
    selected = state;
  }

  protected void doAction() {
    setSelected(!selected);
    super.doAction();
  }
}
