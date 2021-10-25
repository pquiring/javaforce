package javaforce.ui;

/** Component with text content.
 *
 * @author pquiring
 */

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
  private boolean overwrite;

  public TextComponent() {
    text = new char[1][0];
  }

  public String getText() {return new String(text[cur_ln]);}

  public void setText(String text) {
    this.text[cur_ln] = text.toCharArray();
  }

  public int getLength() {
    return text[cur_ln].length;
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
    String text = getText();
    setText(text.substring(0, getSelectionStartOffset()) + text.substring(getSelectionEndOffset()));
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

  private void insert(String str) {
    String text = getText();
    setText(text.substring(0, cur_off) + str + text.substring(cur_off));
  }

  public void showCursor() {
    //ensure cursor is visible (adjust offx)
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
    insert(Character.toString(ch));
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
        String text = getText();
        if (text.length() > 0 && cur_off > 0) {
          setText(text.substring(0, cur_off - 1) + text.substring(cur_off));
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
        String text = getText();
        if (!isEOL()) {
          setText(text.substring(0, cur_off) + text.substring(cur_off + 1));
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
      case 'c':
        if (ctrl) {
          copySelection();
        }
        break;
      case 'v':
        if (ctrl) {
          cutSelection();
        }
        break;
    }
    showCursor();
  }
}
