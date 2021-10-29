package javaforce.ui;

/** Component with text content.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class TextComponent extends FontComponent {
  private char[][] text;
  //selection (start = inclusive : end = exclusive) (similar to String.substring())
  private int sel_start_off = -1;
  private int sel_start_ln = -1;
  private int sel_end_off = -1;
  private int sel_end_ln = -1;
  //cursor
  private int cur_off = 0;
  private int cur_ln = 0;
  //view offset (pixels)
  private int offx = 0;
  private int offy = 0;
  private boolean multi;
  private boolean overwrite;

  public TextComponent(boolean multi) {
    this.multi = multi;
    text = new char[1][0];
  }

  public String getText() {
    return new String(text[cur_ln]);
  }

  public String getLineText(int line) {
    return new String(text[line]);
  }

  public void setText(String text) {
    if (multi) {
      String[] lns = text.replaceAll("\r", "").split("\n");
      int lines = lns.length;
      this.text = new char[lines][];
      for(int line = 0;line < lines;line++) {
        this.text[line] = lns[line].toCharArray();
      }
    } else {
      this.text[cur_ln] = text.toCharArray();
    }
    showCursor();
  }

  public void setLineText(int line, String text) {
    this.text[line] = text.toCharArray();
    showCursor();
  }

  public int getLength() {
    return text[cur_ln].length;
  }

  public int getLineLength(int line) {
    return text[line].length;
  }

  public int getMaxLength() {
    int max = 0;
    for(int a=0;a<text.length;a++) {
      int length = text[a].length;
      if (length > max) {
        max = length;
      }
    }
    return max;
  }

  public int getLineCount() {
    return text.length;
  }

  private void insertLine(int line, char[] newLine) {
    text = JF.copyOfInsert(text, line, newLine);
  }

  private void deleteLine(int line) {
    text = JF.copyOfExcluding(text, line);
  }

  private void splitLine() {
    char[] end = Arrays.copyOfRange(text[cur_ln], cur_off, getLength());
    text[cur_ln] = Arrays.copyOfRange(text[cur_ln], 0, cur_off);
    insertLine(cur_ln + 1, end);
  }

  public int getSelectionStartOffset() {
    return sel_start_off;
  }

  public int getSelectionStartLine() {
    return sel_start_ln;
  }

  public int getSelectionEndOffset() {
    return sel_end_off;
  }

  public int getSelectionEndLine() {
    return sel_end_ln;
  }

  public int getCursorOffset() {
    return cur_off;
  }

  public int getCursorLine() {
    return cur_ln;
  }

  public void setCursorPosition(int offset, int line) {
    cur_ln = line;
    cur_off = offset;
    showCursor();
  }

  public void setCursorOffset(int offset) {
    cur_off = offset;
    showCursor();
  }

  public void setCursorLine(int line) {
    cur_ln = line;
    showCursor();
  }

  public int getViewX() {
    return offx;
  }

  public int getViewY() {
    return offy;
  }

  public boolean isOverwrite() {
    return overwrite;
  }

  public void setOverwrite(boolean state) {
    overwrite = state;
  }

  public boolean isEOL() {
    return cur_off == getLength();
  }

  public boolean haveSelection() {
    return sel_start_off != -1;
  }

  private void deleteSelection() {
    if (!haveSelection()) return;
    if (sel_start_ln == sel_end_ln) {
      //single line selection
      String text = getLineText(cur_ln);
      setLineText(cur_ln, text.substring(0, getSelectionStartOffset()) + text.substring(getSelectionEndOffset()));
    } else {
      for(int line = sel_start_ln;line <= sel_end_ln; line++) {
        if (line == sel_start_ln) {
          //remove trailing part of line
          text[line] = Arrays.copyOfRange(text[line], 0, sel_start_off);
        } else if (line == sel_end_ln) {
          //remove leading part of line
          text[line] = Arrays.copyOfRange(text[line], sel_end_off, text[line].length);
        } else {
          //remove entire line
          deleteLine(line);
        }
      }
    }
    cur_ln = sel_start_ln;
    cur_off = sel_start_off;
    clearSelection();
  }

  private void copySelection() {
    //TODO : see glfw clipboard support
  }

  private void cutSelection() {
    copySelection();
    deleteSelection();
  }

  private void pasteSelection() {
    insert("TODO");
  }

  private void checkSelection() {
    if (sel_start_ln == sel_end_ln) {
      if (sel_start_off == sel_end_off) {
        clearSelection();
        return;
      }
      if (sel_start_off > sel_end_off) {
        //swap offsets
        int tmp = sel_start_off;
        sel_start_off = sel_end_off;
        sel_end_off = tmp;
      }
    } else {
      if (sel_start_ln > sel_end_ln) {
        //swap start/end
        int tmp_ln = sel_start_ln;
        int tmp_off = sel_start_off;
        sel_start_ln = sel_end_ln;
        sel_start_off = sel_end_off;
        sel_end_ln = tmp_ln;
        sel_end_off = tmp_off;
      }
    }
  }

  public void clearSelection() {
    sel_start_off = -1;
    sel_start_ln = -1;
    sel_end_off = -1;
    sel_end_ln = -1;
  }

  public void setSelection(int start, int end) {
    sel_start_off = start;
    sel_start_ln = cur_ln;
    sel_end_off = end;
    sel_end_ln = cur_ln;
  }

  public void setMultiSelection(int start_line, int start_offset, int end_line, int end_offset) {
    sel_start_off = start_offset;
    sel_start_ln = start_line;
    sel_end_off = end_offset;
    sel_end_ln = end_line;
  }

  private void insert(char ch) {
    String text = getLineText(cur_ln);
    setLineText(cur_ln, text.substring(0, cur_off) + Character.toString(ch) + text.substring(cur_off));
  }

  private void insert(String str) {
    //TODO
  }

  public void showCursor() {
    //ensure cursor is not beyond text
    if (cur_ln < 0) {
      cur_ln = 0;
    }
    if (cur_ln > text.length - 1) {
      cur_ln = text.length - 1;
    }
    if (cur_off < 0) {
      cur_off = 0;
    }
    if (cur_off > text[cur_ln].length) {
      cur_off = text[cur_ln].length;
    }
    //ensure cursor is visible
    //adjust offx
    int adv = getFont().getMaxAdvance();
    int cx1 = getCursorOffset() * adv;
    int cx2 = cx1 + adv - 1;
    int w = getWidth() - 2;
    if (cx2 > offx + w) {
      //move view left (offx -)
      offx = cx2 - w;
      if (offx < 0) offx = 0;
    }
    if (cx1 < offx) {
      //move view right (offx +)
      offx = cx1;
    }
    if (!multi) return;
    //adjust offy
    int fonty = getFont().getMaxHeight();
    int cy1 = getCursorLine() * fonty;
    int cy2 = cy1 + fonty - 1;
    int h = getHeight() - 2;
    if (cy2 > offy + h) {
      //move view up (offy -)
      offy = cy2 - h;
      if (offy < 0) offy = 0;
    }
    if (cy1 < offy) {
      //move view down (offy +)
      offy = cy1;
    }
  }

  public void keyTyped(char ch) {
    super.keyTyped(ch);
    if (!isEditable()) return;
    if (haveSelection()) {
      deleteSelection();
    } else {
      if (isOverwrite() && !isEOL()) {
        keyPressed(KeyCode.VK_DELETE);
      }
    }
    insert(ch);
    cur_off++;
    showCursor();
  }

  public void keyPressed(int key) {
    super.keyPressed(key);
    if (!isEditable()) return;
    boolean shift = getKeyState(KeyCode.VK_SHIFT_L) || getKeyState(KeyCode.VK_SHIFT_R);
    boolean ctrl = getKeyState(KeyCode.VK_CONTROL_L) || getKeyState(KeyCode.VK_CONTROL_R);
    switch (key) {
      case KeyCode.VK_BACKSPACE: {
        if (haveSelection()) {
          deleteSelection();
          break;
        }
        String text = getLineText(cur_ln);
        if (text.length() > 0 && cur_off > 0) {
          setLineText(cur_ln, text.substring(0, cur_off - 1) + text.substring(cur_off));
          cur_off--;
        }
        break;
      }
      case KeyCode.VK_DELETE: {
        if (haveSelection()) {
          copySelection();
          deleteSelection();
          break;
        }
        String text = getLineText(cur_ln);
        if (!isEOL()) {
          setLineText(cur_ln, text.substring(0, cur_off) + text.substring(cur_off + 1));
        }
        break;
      }
      case KeyCode.VK_LEFT: {
        if (cur_off == 0) break;
        if (haveSelection()) {
          if (shift) {
            if (sel_start_off == cur_off) {
              sel_start_off--;
            } else {
              sel_end_off--;
              if (sel_start_off == sel_end_off && sel_start_ln == sel_end_ln) {
                clearSelection();
              }
            }
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setSelection(cur_off - 1, cur_off);
          }
        }
        cur_off--;
        break;
      }
      case KeyCode.VK_RIGHT: {
        if (cur_off == getLength()) break;
        if (haveSelection()) {
          if (shift) {
            if (sel_end_off == cur_off) {
              sel_end_off++;
            } else {
              sel_start_off--;
              if (sel_start_off == sel_end_off && sel_start_ln == sel_end_ln) {
                clearSelection();
              }
            }
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setSelection(cur_off, cur_off + 1);
          }
        }
        cur_off++;
        break;
      }
      case KeyCode.VK_UP: {
        if (!multi) break;
        if (cur_ln == 0) break;
        if (haveSelection()) {
          if (shift) {
            if (sel_start_ln == cur_ln) {
              sel_start_ln--;
            } else {
              sel_end_ln--;
            }
            checkSelection();
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setMultiSelection(cur_ln - 1, cur_off, cur_ln, cur_off);
          }
        }
        cur_ln--;
        if (cur_off > getLength()) {
          cur_off = getLength();
        }
        break;
      }
      case KeyCode.VK_DOWN: {
        if (!multi) break;
        if (cur_ln == getLineCount() - 1) break;
        if (haveSelection()) {
          if (shift) {
            if (sel_start_ln == cur_ln) {
              sel_start_ln++;
            } else {
              sel_end_ln++;
            }
            checkSelection();
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setMultiSelection(cur_ln, cur_off, cur_ln + 1, cur_off);
          }
        }
        cur_ln++;
        if (cur_off > getLength()) {
          cur_off = getLength();
        }
        break;
      }
      case KeyCode.VK_HOME: {
        if (cur_off == 0) break;
        if (haveSelection()) {
          if (shift) {
            sel_start_off = 0;
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setSelection(0, cur_off);
          }
        }
        cur_off = 0;
        break;
      }
      case KeyCode.VK_END: {
        int length = getLength();
        if (cur_off == length) break;
        if (haveSelection()) {
          if (shift) {
            sel_end_off = length;
          } else {
            clearSelection();
          }
        } else {
          if (shift) {
            setSelection(cur_off, length);
          }
        }
        cur_off = length;
        break;
      }
      case KeyCode.VK_INSERT: {
        setOverwrite(!isOverwrite());
        break;
      }
      case KeyCode.VK_ENTER: {
        if (!multi) break;
        if (haveSelection()) {
          deleteSelection();
        }
        if (isEOL()) {
          //insert new line after
          cur_ln++;
          insertLine(cur_ln, new char[0]);
        } else if (cur_off == 0) {
          //insert new line before
          insertLine(cur_ln, new char[0]);
          cur_ln++;
        } else {
          //split current line
          splitLine();
          cur_ln++;
        }
        cur_off = 0;
        break;
      }
      case 'x':
        if (ctrl) {
          cutSelection();
        }
        break;
      case 'c':
        if (ctrl) {
          copySelection();
        }
        break;
      case 'v':
        if (ctrl) {
          pasteSelection();
        }
        break;
    }
    showCursor();
  }
}
