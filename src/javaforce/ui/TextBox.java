package javaforce.ui;

/** Text Box
 *
 * @author pquiring
 */

import javaforce.*;

public class TextBox extends TextComponent {
  private Image buffer;

  public TextBox(String text) {
    super(true);
    setText(text);
    setFocusable(true);
    setEditable(true);
  }

  public void render(Image image) {
    int x1 = getX();
    int y1 = getY();
    int w = getWidth();
    int h = getHeight();
    if (w == 0 || h == 0) return;
    int adv = getFont().getMaxAdvance();
    int fonty = getFont().getMaxHeight();
    Dimension size = new Dimension(getMaxLength() * adv, fonty);
    size.width += adv;  //cursor

    if (buffer == null || buffer.getWidth() < size.width || buffer.getHeight() != size.height) {
      buffer = new Image(size.width, size.height);
    }
    int bw = buffer.getWidth();
    int bh = buffer.getHeight();
    image.setForeColor(getForeColor());
    image.drawBox(x1, y1, w, h);
    //remove border line
    x1++;
    y1++;
    w -= 2;
    h -= 2;
    int x2 = x1 + w - 1;
    int y2 = y1 + h - 1;
    int lineheight = size.height;
    int lines = getLineCount();
    int cursor_line = getCursorLine();
    int line_start = getViewY() / lineheight;
    int clip_y = getViewY() % lineheight;
    for(int line = line_start;line < lines;line++) {
      buffer.fill(0, 0, size.width, size.height, getBackColor().getColor());
      if (haveSelection()) {
        //draw selection
        int sx1 = -1, sx2 = -1;
        if (line == getSelectionStartLine() && line == getSelectionEndLine()) {
          //selection only on this line
          sx1 = getSelectionStartOffset() * adv;
          sx2 = getSelectionEndOffset() * adv - 1;
        } else if (line == getSelectionStartLine()) {
          //selection starts on this line
          sx1 = getSelectionStartOffset() * adv;
          sx2 = getLineLength(line) * adv;
        } else if (line == getSelectionEndLine()) {
          //selection ends on this line
          sx1 = 0;
          sx2 = getSelectionEndOffset() * adv - 1;
        } else {
          //line is fully selected
          sx1 = 0;
          sx2 = getLineLength(line) * adv;
        }
        int sel_width = sx2 - sx1 + 1;
        buffer.fill(sx1, 0, sel_width, bh, getSelectedColor().getColor());
      }
      buffer.setForeColor(getForeColor());
      buffer.drawText(0, -getFont().getMaxAscent(), getLineText(line));
      //draw cursor on buffer
      if (isFocused() && getFont().showCursor() && cursor_line == line) {
        int cx1 = getCursorOffset() * adv;
        int cx2 = isOverwrite() ? (cx1 + adv - 1) : (cx1 + 2);
        int cy1 = 0;
        int cy2 = getFont().getMaxHeight() - 1;
        int cw = cx2 - cx1 + 1;
        int ch = cy2 - cy1 + 1;
        buffer.fill(cx1, cy1, cw, ch);
      }
      //clipping
      if (getViewX() + w >= bw) {
        //right side
        w = bw - getViewX();
      }
      if (y1 + lineheight > y2) {
        //bottom (last line)
        lineheight = y2 - y1 + 1;
        if (lineheight <= 0) break;
      }
      image.putPixelsBlend(buffer.getBuffer(), x1, y1, w, lineheight - clip_y, getViewX() + clip_y * bw, bw, true);
      if (clip_y > 0) {
        y1 += lineheight - clip_y;
        clip_y = 0;
      } else {
        y1 += lineheight;
      }
    }
  }
}
