package javaforce.awt;

/** VNCJavaRobot
 *
 * @author peter.quiring
 */

import java.awt.*;

import javaforce.*;

public class VNCJavaRobot implements VNCRobot {

  private Robot robot;
  private GraphicsDevice screen;

  public VNCJavaRobot(GraphicsDevice screen) {
    this.screen = screen;
    try {
      robot = new Robot();
    } catch (Exception e) {
      JFLog.log(e);
    }

  }

  public Rectangle getScreenSize() {
    return screen.getDefaultConfiguration().getBounds();
  }

  public JFImage getScreenCatpure() {
    return JFImage.createScreenCapture(screen);
  }

  public void keyPress(int code) {
    robot.keyPress(code);
  }

  public void keyRelease(int code) {
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
}
