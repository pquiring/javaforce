/*
 * Hex.java
 *
 * Created on May 30, 2008, 4:45 PM
 *
 * @author pquiring
 *
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;

public class Hex extends JComponent implements KeyListener/*, MouseListener*/ {

  public Hex(jfhexbig jh) {
    this.jh = jh;
    setFocusable(true);
    data = new byte[512];
    length = 0;
    addKeyListener(this);
    changeFont(Settings.fnt);
  }

  //public data
  public byte data[];
  public int length;

  //private data
  private jfhexbig jh;
  private static int fx, fy;  //font size x/y
  private static int baseline;
  private static Color foreColor = Color.BLACK, backColor = Color.WHITE;
  private boolean cursorShown = false;
  private int selectStart = -1, selectEnd = -1;
  private static Color cursorColor = new Color(0x7777ff);
  private static Color selectColor = new Color(0x77ff77);
  private int cx, cy;  //cursor pos
  private boolean leftSide;  //on left side (SPACE switches)
  private boolean leftNibble;  //on left Nibble (if on left side) (a nibble is 4 bits 0xa thru 0xf)
  private boolean insertMode = false;
  private final int INF = 0x7fff;

  public static void changeFont(Font newFont) {
    int metrics[] = JFAWT.getFontMetrics(Settings.fnt);
    fx = metrics[0];
    fy = metrics[1] + metrics[2];
    baseline = metrics[2];
  }

  public void setData(byte in[]) {
    length = in.length;
    System.arraycopy(in, 0, data, 0, length);
    cx = cy = 0;
    leftSide = true;
    leftNibble = true;
    selectStart = selectEnd = -1;
    setSize(getPreferredSize());
    invalidate();
    repaint();
  }

  public byte[] getData() {
    byte ret[] = new byte[length];
    System.arraycopy(data, 0, ret, 0, length);
    return ret;
  }

  public void setForeColor(int newClr) {
    foreColor = new Color(newClr);
  }
  public void setBackColor(int newClr) {
    backColor = new Color(newClr);
  }
  public void setForeColor(Color newClr) {
    foreColor = newClr;
  }
  public void setBackColor(Color newClr) {
    backColor = newClr;
  }
  public Color getForeColor() { return foreColor; }
  public Color getBackColor() { return backColor; }

  public void update(boolean showCursor) {
    if (showCursor) scrollRectToVisible(new Rectangle(0,cy * fy,fx,fy));
    repaint();
  }

//0        9                                               57
//01234567 xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx ################
  public void paintComponent(Graphics g) {
    Rectangle r = g.getClipBounds();
    int starty;
    int endy;
    starty = r.y / fy;
    endy = (r.y + r.height) / fy + 1;
    g.setFont(Settings.fnt);
    char ch;
    //clear
    g.setColor(backColor);
    g.fillRect(r.x, r.y, r.width, r.height);
    //draw cursor
    g.setColor(cursorColor);
    if (leftSide) {
      if (insertMode)
        g.fillRect((cx * 3 + 9 + (leftNibble ? 0 : 1)) * fx,cy * fy,fx,fy);
      else
        g.drawRect((cx * 3 + 9 + (leftNibble ? 0 : 1)) * fx,cy * fy,fx,fy);
      } else {
      if (insertMode)
        g.fillRect((cx + 57) * fx,cy * fy,fx,fy);
      else
        g.drawRect((cx + 57) * fx,cy * fy,fx,fy);
    }
    g.setColor(foreColor);
    int cp = cy * 16 + cx;
    for(int y = starty;y < endy;y++) {
      g.drawString(String.format("%08x",y * 16), 0, (y+1) * fy - 1 - baseline);
      for(int x = 0;x < 16;x++) {
        int p = y * 16 + x;
        if (p >= length) return;
        if (((p >= selectStart) && (p <= selectEnd)) || ((p >= selectEnd) && (p <= selectStart) && (selectEnd >= 0))) {
          g.setColor(selectColor);
          g.fillRect((x * 3 + 9) * fx,y * fy,fx*2,fy);
          g.fillRect((x + 57) * fx,y * fy,fx,fy);
          if (p == cp) {
            //draw cursor (over selection)
            g.setColor(cursorColor);
            if (leftSide) {
              if (insertMode)
                g.fillRect((cx * 3 + 9 + (leftNibble ? 0 : 1)) * fx,cy * fy,fx,fy);
              else
                g.drawRect((cx * 3 + 9 + (leftNibble ? 0 : 1)) * fx,cy * fy,fx,fy);
            } else {
              if (insertMode)
                g.fillRect((cx + 57) * fx,cy * fy,fx,fy);
              else
                g.drawRect((cx + 57) * fx,cy * fy,fx,fy);
            }
          }
          g.setColor(foreColor);
        }
        ch = (char)(((int)data[p]) & 0xff);
        g.drawString(String.format("%x", (ch & 0xf0) >> 4), (x * 3 + 9) * fx + fx/4,(y+1) * fy - 1 - baseline);
        g.drawString(String.format("%x", ch & 0x0f), (x * 3 + 10) * fx + fx/4,(y+1) * fy - 1 - baseline);
        if (ch == 0) continue;
        if (ch > 127) ch = ASCII8.convert(ch);
        g.drawString("" + (char)ch, (x + 57) * fx,(y+1) * fy - 1 - baseline);
      }
    }
  }

  public void copy() {
    int tmp;
    if ((selectStart == -1) || (selectEnd == -1)) return;  //nothing to copy
    try {
      if (selectStart > selectEnd) {
        tmp = selectStart;
        selectStart = selectEnd;
        selectEnd = tmp;
      }
      String str = new String(data, selectStart, selectEnd+1);
      StringSelection ss = new StringSelection(str);
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (cb == null) return;
      cb.setContents(ss, ss);
    } catch (Exception e) {}
  }

  public void paste() {
    try {
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      String str = (String)cb.getContents(null).getTransferData(DataFlavor.stringFlavor);
      if (str == null) return;
      boolean orgleftSide = leftSide;
      leftSide = false;
      insert(str.toCharArray());
      leftSide = orgleftSide;
    } catch (Exception e) {}
    jh.changed();
    repaint();
  }

  private void data_delete(int start, int end) {
    System.arraycopy(data, end, data, start, 512 - end);
    Arrays.fill(data, end+1, 511, (byte)0);
  }

  public void delete() {
    if (selectStart == -1) return;
    copy();
    data_delete(selectStart, selectEnd+1);
    setCaretPosition(selectStart);
    selectStart = selectEnd = -1;
    jh.changed();
    repaint();
  }

/*
  public void cut() {
    if (selectStart == -1) return;
    int tmp;
    if (selectStart > selectEnd) {
      tmp = selectStart;
      selectStart = selectEnd;
      selectEnd = tmp;
    }
    txt.delete(selectStart, selectEnd+1);
    setCaretPosition(selectStart);
    selectStart = selectEnd = -1;
    jh.changed();
    repaint();
  }
*/

  public void paste(char str[]) {
    try {
      boolean orgleftSide = leftSide;
      leftSide = false;
      insert(str);
      leftSide = orgleftSide;
    } catch (Exception e) {}
    jh.changed();
    repaint();
  }

  public boolean eof() {
    return (cy * 16 + cx) == length;
  }

  private void data_insert(int pos, char ch) {
    if (pos == 512) return;
    if (length < 512) length++;
    if (pos < 511) System.arraycopy(data, pos, data, pos+1, 511 - pos);
    data[pos] = (byte)ch;
  }

  public void insert(char ch) {
    if (ch == 9) return;
    if (leftSide) {
      //can only type 0-9,a-f
      ch = Character.toLowerCase(ch);
      int value = 0;
      if ((ch >= '0') && (ch <= '9')) {
        value = ch - '0';
      } else if ((ch >= 'a') && (ch <= 'f')) {
        value = ch - ('a' - 10);
      } else return;
      if (leftNibble) {
        ch = (char)(value << 4);
        data_insert(cy * 16 + cx, ch);
      } else {
        if (!eof()) value += data[cy * 16 + cx] & 0xf0;
        ch = (char)value;
        if (!eof()) data[cy * 16 + cx] = (byte)ch; else data_insert(cy * 16 + cx, ch);
      }
    } else {
      data_insert(cy * 16 + cx, ch);
    }
    move(1, 0, false);
    jh.changed();
  }

  public void insert(char chs[]) {
    for(int a=0;a<chs.length;a++) insert(chs[a]);
  }

  public void overwrite(char ch) {
    if (cy == 32) return;  //512 bytes
    if (leftSide) {
      //can only type 0-9,a-f
      ch = Character.toLowerCase(ch);
      int value = 0;
      if ((ch >= '0') && (ch <= '9')) {
        value = ch - '0';
      } else if ((ch >= 'a') && (ch <= 'f')) {
        value = ch - ('a' - 10);
      } else return;
      if (leftNibble) {
        value <<= 4;
        if (!eof()) value += data[cy * 16 + cx] & 0x0f;
        ch = (char)(value);
        if (!eof()) data[cy * 16 + cx] = (byte)ch; else data_insert(cy * 16 + cx, ch);
      } else {
        value += data[cy * 16 + cx] & 0xf0;
        ch = (char)value;
        if (!eof()) data[cy * 16 + cx] = (byte)ch; else data_insert(cy * 16 + cx, ch);
      }
    } else {
      data[cy * 16 + cx] = (byte)ch;
    }
    move(1, 0, false);
    jh.changed();
  }

  public void overwrite(char chs[]) {
    for(int a=0;a<chs.length;a++) overwrite(chs[a]);
  }

  public void switchSide() {
    if (leftSide) leftSide = false; else leftSide = true;
    leftNibble = true;
    update(true);
  }

  public void switchInsertMode() {
//    if (insertMode) insertMode = false; else insertMode = true;
    update(true);
  }

  public Dimension getPreferredSize() {
    int x = (57 + 16) * fx;
    int y = ((length / 16) + 1) * fy;
    return new Dimension(x,y);
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  //Note : abs(x) can not be > 1
  public void move(int x, int y, boolean selecting) {
    if (selecting) {
      if (selectStart == -1) selectStart = cy * 16 + cx;
    } else {
      selectStart = selectEnd = -1;
    }
    if (x != 0) {
      if (leftSide) {
        if (leftNibble) {
          leftNibble = false;
          if (x == 1) x = 0;
        } else {
          leftNibble = true;
          if (x == -1) x = 0;
        }
      }
      cx += x;
      if (cx == -1) {cx = 15; cy--;}
      if (cx == 16) {cx = 0; cy++;}
    }
    cy += y;
    if ((cy < 0) || (y == -INF)) {cx = 0; cy = 0; leftNibble = true;}
    if ((cy * 16 + cx > length) || (y == INF)) {
      cy = length / 16;
      cx = length - (cy * 16);
    }
    if (selecting) selectEnd = cy * 16 + cx;
    update(true);
  }

  public void setCaretPosition(int pos) {
    if ((pos < 0) || (pos > length)) {
      int len = length;
      cy = len / 16;
      cx = len - (cy * 16);
    } else {
      cy = pos / 16;
      cx = pos - (cy * 16);
    }
    update(true);
  }

  public int getCaretPosition() {
    return cy * 16 + cx;
  }

  public void select(int start, int end) {
    selectStart = start;
    selectEnd = end;
    setCaretPosition(start);
  }

  public boolean isSelect() {
    return selectStart != -1;
  }

//interface KeyListener
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    int keyMods = e.getModifiers();
    if (keyMods == KeyEvent.CTRL_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_A: selectStart = 0; selectEnd = length; break;
        case KeyEvent.VK_C:  //no break
        case KeyEvent.VK_INSERT: copy(); break;
        case KeyEvent.VK_V: paste(); break;
        case KeyEvent.VK_HOME: move(0, -INF, false); e.consume(); break;
        case KeyEvent.VK_X: delete(); break;
        case KeyEvent.VK_END: move(0, INF, false); e.consume(); break;
      }
    }
    if (keyMods == (KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) {
      switch (keyCode) {
        case KeyEvent.VK_HOME: move(0, -INF, true); e.consume(); break;
        case KeyEvent.VK_END: move(0, INF, true); e.consume(); break;
      }
    }
    if (keyMods == KeyEvent.SHIFT_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_UP: move(0, -1, true); e.consume(); break;
        case KeyEvent.VK_DOWN: move(0, 1, true); e.consume(); break;
        case KeyEvent.VK_LEFT: move(-1, 0, true); e.consume(); break;
        case KeyEvent.VK_RIGHT: move(1, 0, true); e.consume(); break;
        case KeyEvent.VK_PAGE_UP: move(0, -16, true); e.consume(); break;
        case KeyEvent.VK_PAGE_DOWN: move(0, 16, true); e.consume(); break;
        case KeyEvent.VK_INSERT: paste(); break;
        case KeyEvent.VK_DELETE: delete(); break;
      }
    }
    if (keyMods == 0) {
      switch (keyCode) {
        case KeyEvent.VK_UP: move(0, -1, false); e.consume(); break;
        case KeyEvent.VK_DOWN: move(0, 1, false); e.consume(); break;
        case KeyEvent.VK_LEFT: move(-1, 0, false); e.consume(); break;
        case KeyEvent.VK_RIGHT: move(1, 0, false); e.consume(); break;
        case KeyEvent.VK_PAGE_UP: move(0, -16, false); e.consume(); break;
        case KeyEvent.VK_PAGE_DOWN: move(0, 16, false); e.consume(); break;
        case KeyEvent.VK_SPACE: switchSide(); e.consume(); break;
        case KeyEvent.VK_INSERT: switchInsertMode(); break;
        case KeyEvent.VK_DELETE: {
          if (selectStart != -1) {
//            cut();
          } else {
            if ((length > 1) && (!eof())) {
              data_delete(getCaretPosition(), getCaretPosition()+1);
              jh.changed();
              repaint();
            }
          }
        }
      }
    }
  }
  public void keyReleased(KeyEvent e) {
  }
  public void keyTyped(KeyEvent e) {
    if (e.getModifiers() == KeyEvent.CTRL_MASK) return;
    if (e.getModifiers() == KeyEvent.ALT_MASK) return;
    if (e.getModifiers() == (KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK)) return;
    char key = e.getKeyChar();
    switch (key) {
      case 8:  //backspace
      case 0x7f:  //delete
        return;
    }
    if (selectStart != -1) delete();
    if (insertMode) insert(key); else overwrite(key);
  }
/*
  public void mouseClicked(MouseEvent e) { System.out.println("clicked"); grabFocus(); }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mousePressed(MouseEvent e) { }
  public void mouseReleased(MouseEvent e) { }
*/
}
