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
  public static int VK_ENTER = 10;
  public static int VK_TAB = 9;
  public static int VK_CONTROL = 17;
  public static int VK_ALT = 18;
  public static int VK_SHIFT = 16;
  public static int VK_ESCAPE = 27;
  public static int VK_ARROW_LEFT = 37;
  public static int VK_ARROW_UP = 38;
  public static int VK_ARROW_RIGHT = 39;
  public static int VK_ARROW_DOWN = 40;
  public static int VK_INSERT = 155;
  public static int VK_DELETE = 127;
  public static int VK_HOME = 36;
  public static int VK_END = 35;
  public static int VK_PAGE_UP = 33;
  public static int VK_PAGE_DOWN = 34;
  public static int VK_BACK_SPACE = 8;
}
