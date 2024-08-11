package javaforce.awt;

/** VNCJavaRobot
 *
 * @author peter.quiring
 */

import java.awt.*;
import java.awt.event.*;

import javaforce.*;

public class VNCJavaRobot implements VNCRobot {

  private Robot robot;
  private GraphicsDevice screen;

  public VNCJavaRobot(GraphicsDevice screen) {
    this.screen = screen;
    try {
      robot = new Robot(screen);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public Rectangle getScreenSize() {
    return screen.getDefaultConfiguration().getBounds();
  }

  public int[] getScreenCapture() {
    return JFImage.createScreenCapture(screen).getBuffer();
  }

  public void keyPress(int code) {
    code = convertRFBKeyCode(code);
    robot.keyPress(code);
  }

  public void keyRelease(int code) {
    code = convertRFBKeyCode(code);
    robot.keyRelease(code);
  }

  public void mouseMove(int x, int y) {
    robot.mouseMove(x, y);
  }

  public void mousePress(int button) {
    robot.mousePress(button);
  }

  public void mouseRelease(int button) {
    robot.mouseRelease(button);
  }

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
        key = KeyEvent.VK_SHIFT;
        break;
      case RFB.VK_CONTROL:
        key = KeyEvent.VK_CONTROL;
        break;
      case RFB.VK_META:
        key = KeyEvent.VK_META;
        break;
      case RFB.VK_ALT:
        key = KeyEvent.VK_ALT;
        break;
      case RFB.VK_DELETE:
        key = KeyEvent.VK_DELETE;
        break;
    }
    return key;
  }

  public void close() {}
}
