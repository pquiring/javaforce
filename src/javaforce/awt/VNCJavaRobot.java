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
    code = VNCRobot.convertRFBKeyCode(code);
    try {
      robot.keyPress(code);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void keyRelease(int code) {
    code = VNCRobot.convertRFBKeyCode(code);
    try {
      robot.keyRelease(code);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mouseMove(int x, int y) {
    try {
      robot.mouseMove(x, y);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mousePress(int button) {
    button = VNCRobot.convertMouseButtons(button);
    try {
      robot.mousePress(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mouseRelease(int button) {
    button = VNCRobot.convertMouseButtons(button);
    try {
      robot.mouseRelease(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {}
}
