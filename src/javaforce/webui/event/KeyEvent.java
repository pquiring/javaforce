package javaforce.webui.event;

/** KeyEvent
 *
 * @author pquiring
 */

public class KeyEvent {
  public String action;  //up, down
  public boolean ctrlKey, altKey, shiftKey;
  public int keyCode;
  public char keyChar;

  //VK codes
  public static final int VK_ENTER = 10;
  public static final int VK_TAB = 9;
  public static final int VK_CONTROL = 17;
  public static final int VK_ALT = 18;
  public static final int VK_SHIFT = 16;
  public static final int VK_ESCAPE = 27;
  public static final int VK_ARROW_LEFT = 37;
  public static final int VK_ARROW_UP = 38;
  public static final int VK_ARROW_RIGHT = 39;
  public static final int VK_ARROW_DOWN = 40;
  public static final int VK_INSERT = 155;
  public static final int VK_DELETE = 127;
  public static final int VK_HOME = 36;
  public static final int VK_END = 35;
  public static final int VK_PAGE_UP = 33;
  public static final int VK_PAGE_DOWN = 34;
  public static final int VK_BACK_SPACE = 8;
  public static final int VK_PAUSE = 0x13;

  public static final int VK_F1 = 0x70;
  public static final int VK_F2 = 0x71;
  public static final int VK_F3 = 0x72;
  public static final int VK_F4 = 0x73;
  public static final int VK_F5 = 0x74;
  public static final int VK_F6 = 0x75;
  public static final int VK_F7 = 0x76;
  public static final int VK_F8 = 0x77;
  public static final int VK_F9 = 0x78;
  public static final int VK_F10 = 0x79;
  public static final int VK_F11 = 0x7a;
  public static final int VK_F12 = 0x7b;

  public String toString() {
    return "KeyEvent:keyCode=" + keyCode + ":keyChar=" + (int)keyChar;
  }
}
