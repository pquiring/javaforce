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

  public int[] getScreenCapture(int pf) {
    int[] rgb = JFImage.createScreenCapture(screen).getBuffer();
    if (pf == RFB.PF_RGB) return rgb;
    return RFB.swapPixelFormat(rgb);
  }

  public void keyPress(int code) {
    try {
      robot.keyPress(code);
    } catch (Exception e) {
      JFLog.log(e);
      JFLog.log("invalid code=" + code);
    }
  }

  public void keyRelease(int code) {
    try {
      robot.keyRelease(code);
    } catch (Exception e) {
      JFLog.log(e);
      JFLog.log("invalid code=" + code);
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
    try {
      robot.mousePress(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mouseRelease(int button) {
    try {
      robot.mouseRelease(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {}
}
