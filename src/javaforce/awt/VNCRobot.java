package javaforce.awt;

/** VNCRobot
 *
 * @author peter.quiring
 */

import java.awt.*;
import java.awt.event.*;

import javaforce.*;

public interface VNCRobot {
  public Rectangle getScreenSize();
  public int[] getScreenCapture(int pf);
  public void keyPress(int code);
  public void keyRelease(int code);
  public void mouseMove(int x, int y);
  public void mousePress(int button);
  public void mouseRelease(int button);
  public boolean active();
  public void close();

  /** Convert Java key code to RFB key code. */
  public static int convertJavaKeyCode(int key) {
    switch (key) {
      case KeyEvent.VK_BACK_SPACE:
        key = RFB.VK_BACK_SPACE;
        break;
      case KeyEvent.VK_TAB:
        key = RFB.VK_TAB;
        break;
      case KeyEvent.VK_ENTER:
        key = RFB.VK_ENTER;
        break;
      case KeyEvent.VK_ESCAPE:
        key = RFB.VK_ESCAPE;
        break;
      case KeyEvent.VK_HOME:
        key = RFB.VK_HOME;
        break;
      case KeyEvent.VK_LEFT:
        key = RFB.VK_LEFT;
        break;
      case KeyEvent.VK_UP:
        key = RFB.VK_RIGHT;
        break;
      case KeyEvent.VK_RIGHT:
        key = RFB.VK_RIGHT;
        break;
      case KeyEvent.VK_DOWN:
        key = RFB.VK_DOWN;
        break;
      case KeyEvent.VK_PAGE_UP:
        key = RFB.VK_PAGE_UP;
        break;
      case KeyEvent.VK_PAGE_DOWN:
        key = RFB.VK_PAGE_DOWN;
        break;
      case KeyEvent.VK_END:
        key = RFB.VK_END;
        break;
      case KeyEvent.VK_INSERT:
        key = RFB.VK_INSERT;
        break;
      case KeyEvent.VK_F1:
        key = RFB.VK_F1;
        break;
      case KeyEvent.VK_F2:
        key = RFB.VK_F2;
        break;
      case KeyEvent.VK_F3:
        key = RFB.VK_F3;
        break;
      case KeyEvent.VK_F4:
        key = RFB.VK_F4;
        break;
      case KeyEvent.VK_F5:
        key = RFB.VK_F5;
        break;
      case KeyEvent.VK_F6:
        key = RFB.VK_F6;
        break;
      case KeyEvent.VK_F7:
        key = RFB.VK_F7;
        break;
      case KeyEvent.VK_F8:
        key = RFB.VK_F8;
        break;
      case KeyEvent.VK_F9:
        key = RFB.VK_F9;
        break;
      case KeyEvent.VK_F10:
        key = RFB.VK_F10;
        break;
      case KeyEvent.VK_F11:
        key = RFB.VK_F11;
        break;
      case KeyEvent.VK_F12:
        key = RFB.VK_F12;
        break;
      case KeyEvent.VK_SHIFT:
        key = RFB.VK_SHIFT;
        break;
      case KeyEvent.VK_CONTROL:
        key = RFB.VK_CONTROL;
        break;
      case KeyEvent.VK_META:
        key = RFB.VK_META;
        break;
      case KeyEvent.VK_ALT:
        key = RFB.VK_ALT;
        break;
      case KeyEvent.VK_DELETE:
        key = RFB.VK_DELETE;
        break;
    }
    return key;
  }

  /** Convert RFB key code to Java key code. */
  public static int convertRFBKeyCode(int key) {
    if (key >= 'a' && key <= 'z') {
      //convert to upper case
      key -= ('a' - 'A');
    }
    switch (key) {
      case RFB.VK_BACK_SPACE:
        key = KeyEvent.VK_BACK_SPACE;
        break;
      case RFB.VK_TAB:
        key = KeyEvent.VK_TAB;
        break;
      case RFB.VK_ENTER:
        key = KeyEvent.VK_ENTER;
        break;
      case RFB.VK_ESCAPE:
        key = KeyEvent.VK_ESCAPE;
        break;
      case RFB.VK_HOME:
        key = KeyEvent.VK_HOME;
        break;
      case RFB.VK_LEFT:
        key = KeyEvent.VK_LEFT;
        break;
      case RFB.VK_UP:
        key = KeyEvent.VK_UP;
        break;
      case RFB.VK_RIGHT:
        key = KeyEvent.VK_RIGHT;
        break;
      case RFB.VK_DOWN:
        key = KeyEvent.VK_DOWN;
        break;
      case RFB.VK_PAGE_UP:
        key = KeyEvent.VK_PAGE_UP;
        break;
      case RFB.VK_PAGE_DOWN:
        key = KeyEvent.VK_PAGE_DOWN;
        break;
      case RFB.VK_END:
        key = KeyEvent.VK_END;
        break;
      case RFB.VK_INSERT:
        key = KeyEvent.VK_INSERT;
        break;
      case RFB.VK_F1:
        key = KeyEvent.VK_F1;
        break;
      case RFB.VK_F2:
        key = KeyEvent.VK_F2;
        break;
      case RFB.VK_F3:
        key = KeyEvent.VK_F3;
        break;
      case RFB.VK_F4:
        key = KeyEvent.VK_F4;
        break;
      case RFB.VK_F5:
        key = KeyEvent.VK_F5;
        break;
      case RFB.VK_F6:
        key = KeyEvent.VK_F6;
        break;
      case RFB.VK_F7:
        key = KeyEvent.VK_F7;
        break;
      case RFB.VK_F8:
        key = KeyEvent.VK_F8;
        break;
      case RFB.VK_F9:
        key = KeyEvent.VK_F9;
        break;
      case RFB.VK_F10:
        key = KeyEvent.VK_F10;
        break;
      case RFB.VK_F11:
        key = KeyEvent.VK_F11;
        break;
      case RFB.VK_F12:
        key = KeyEvent.VK_F12;
        break;
      case RFB.VK_SHIFT:
      case RFB.VK_SHIFT_R:
        key = KeyEvent.VK_SHIFT;
        break;
      case RFB.VK_CONTROL:
      case RFB.VK_CONTROL_R:
        key = KeyEvent.VK_CONTROL;
        break;
      case RFB.VK_META:
      case RFB.VK_META_R:
        key = KeyEvent.VK_META;
        break;
      case RFB.VK_ALT:
      case RFB.VK_ALT_R:
        key = KeyEvent.VK_ALT;
        break;
      case RFB.VK_DELETE:
        key = KeyEvent.VK_DELETE;
        break;
      case RFB.VK_EXCLAMATION_MASK:
        key = KeyEvent.VK_1;
        break;
      case RFB.VK_AT:
        key = KeyEvent.VK_2;
        break;
      case RFB.VK_NUMBER_SIGN:
        key = KeyEvent.VK_3;
        break;
      case RFB.VK_DOLLAR_SIGN:
        key = KeyEvent.VK_4;
        break;
      case RFB.VK_PERCENT:
        key = KeyEvent.VK_5;
        break;
      case RFB.VK_CIRCUMFLEX:
        key = KeyEvent.VK_6;
        break;
      case RFB.VK_AMPERSAND:
        key = KeyEvent.VK_7;
        break;
      case RFB.VK_ASTERISK:
        key = KeyEvent.VK_8;
        break;
      case RFB.VK_LEFT_PARENTHSIS:
        key = KeyEvent.VK_9;
        break;
      case RFB.VK_RIGHT_PARENTHSIS:
        key = KeyEvent.VK_0;
        break;
      case RFB.VK_UNDERSCORE:
        key = KeyEvent.VK_MINUS;
        break;
      case RFB.VK_PLUS:
        key = KeyEvent.VK_EQUALS;
        break;
      case RFB.VK_QUOTE_LEFT:
      case RFB.VK_TILDE:
        key = KeyEvent.VK_BACK_QUOTE;
        break;
      case RFB.VK_NUMPAD0:
        key = KeyEvent.VK_0;
        break;
      case RFB.VK_NUMPAD1:
        key = KeyEvent.VK_1;
        break;
      case RFB.VK_NUMPAD2:
        key = KeyEvent.VK_2;
        break;
      case RFB.VK_NUMPAD3:
        key = KeyEvent.VK_3;
        break;
      case RFB.VK_NUMPAD4:
        key = KeyEvent.VK_4;
        break;
      case RFB.VK_NUMPAD5:
        key = KeyEvent.VK_5;
        break;
      case RFB.VK_NUMPAD6:
        key = KeyEvent.VK_6;
        break;
      case RFB.VK_NUMPAD7:
        key = KeyEvent.VK_7;
        break;
      case RFB.VK_NUMPAD8:
        key = KeyEvent.VK_8;
        break;
      case RFB.VK_NUMPAD9:
        key = KeyEvent.VK_9;
        break;
      case RFB.VK_NUMPAD_ENTER:
        key = KeyEvent.VK_ENTER;
        break;
      case RFB.VK_NUMPAD_ASTERISK:
        key = KeyEvent.VK_MULTIPLY;
        break;
      case RFB.VK_NUMPAD_PLUS:
        key = KeyEvent.VK_ADD;
        break;
      case RFB.VK_NUMPAD_PERIOD:
        key = KeyEvent.VK_PERIOD;
        break;
      case RFB.VK_NUMPAD_MINUS:
        key = KeyEvent.VK_SUBTRACT;
        break;
      case RFB.VK_NUMPAD_DIVIDE:
        key = KeyEvent.VK_DIVIDE;
        break;
      case RFB.VK_OPEN_BRACKET:
        key = KeyEvent.VK_OPEN_BRACKET;
        break;
      case RFB.VK_CLOSE_BRACKET:
        key = KeyEvent.VK_CLOSE_BRACKET;
        break;
      case RFB.VK_PIPE:
        key = KeyEvent.VK_BACK_SLASH;
        break;
      case RFB.VK_SEMICOLON:
        key = KeyEvent.VK_SEMICOLON;
        break;
      case RFB.VK_DOUBLE_QUOTE:
        key = KeyEvent.VK_QUOTE;
        break;
      case RFB.VK_LESS:
        key = KeyEvent.VK_COMMA;
        break;
      case RFB.VK_GREATER:
        key = KeyEvent.VK_PERIOD;
        break;
      case RFB.VK_QUESTION_MARK:
        key = KeyEvent.VK_SLASH;
        break;
      case RFB.VK_QUOTE:
        key = KeyEvent.VK_QUOTE;
        break;
      case RFB.VK_WIN_KEY:
      case RFB.VK_WIN_KEY_R:
        key = KeyEvent.VK_WINDOWS;
        break;
      case RFB.VK_CONTEXT_MENU:
        key = KeyEvent.VK_CONTEXT_MENU;
        break;
    }
    return key;
  }

  /** Convert RFB buttons to java.awt.Robot buttons. */
  public static int convertMouseButtons(int button) {
    switch (button) {
      case 1: return InputEvent.BUTTON1_DOWN_MASK;
      case 2: return InputEvent.BUTTON2_DOWN_MASK;
      case 4: return InputEvent.BUTTON3_DOWN_MASK;
      default: {
        JFLog.log("Unknown mouse button:" + button);
        return 0;
      }
    }
  }
}
