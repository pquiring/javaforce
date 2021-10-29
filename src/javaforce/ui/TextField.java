package javaforce.ui;

/** Text Field
 *
 * @author pquiring
 */

import javaforce.*;

public class TextField extends TextComponent {

  private Image buffer;
  private boolean password;

  public TextField(String text) {
    super(false);
    setText(text);
    setFocusable(true);
    setEditable(true);
  }

  public Dimension getMinSize() {
    int w = getWidth();
    int h = getFont().getMetrics(getText()).getHeight();
    return new Dimension(w, h);
  }

  public void render(Image image) {
    int x = getX();
    int y = getY();
    int w = getWidth();
    int h = getHeight();
    if (w == 0) return;
    int adv = getFont().getMaxAdvance();
    Dimension size = getFont().getMetrics(getText()).toDimension();
    if (size.width < getWidth()) size.width = getWidth();
    size.width += adv;  //cursor

    if (buffer == null || buffer.getWidth() != size.width || buffer.getHeight() != size.height) {
      buffer = new Image(size.width, size.height);
    }
    image.setForeColor(getForeColor());
    image.drawBox(x, y, w, h);
    //remove border line
    w -= 2;
    h -= 2;
    buffer.fill(0, 0, size.width, size.height, getBackColor().getColor());
    if (haveSelection()) {
      //draw selection
      int x1 = getSelectionStartOffset() * adv;
      int x2 = getSelectionEndOffset() * adv - 1;
      int sel_width = x2 - x1 + 1;
      buffer.fill(x1, 0, sel_width, size.height, getSelectedColor().getColor());
    }
    buffer.setForeColor(getForeColor());
    if (password) {
      int len = getLength();
      StringBuilder stars = new StringBuilder();
      for(int a=0;a<len;a++) {
        stars.append('*');
      }
      buffer.drawText(0, -getFont().getMaxAscent(), stars.toString());
    } else {
      buffer.drawText(0, -getFont().getMaxAscent(), getText());
    }
    //draw cursor on buffer
    if (isFocused() && getFont().showCursor()) {
      int cx1 = getCursorOffset() * adv;
      int cx2 = isOverwrite() ? (cx1 + adv - 1) : (cx1 + 2);
      int cy1 = 0;
      int cy2 = getFont().getMaxHeight() - 1;
      int cw = cx2 - cx1 + 1;
      int ch = cy2 - cy1 + 1;
      buffer.fill(cx1, cy1, cw, ch);
    }
    int cx = 1;
    int cy = (size.height - getHeight()) / 2 + 1;
    if (getViewX() + w > size.width) {
      w = size.width - getViewX();
    }
    image.putPixelsBlend(buffer.getBuffer(), x + cx, y + cy, w, h, getViewX(), size.width, true);
  }

  public boolean isPassword() {
    return password;
  }

  public void setPassword(boolean state) {
    password = state;
  }

  public String toString() {
    return "TextField:" + getText();
  }
}
