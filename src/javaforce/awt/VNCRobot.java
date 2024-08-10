package javaforce.awt;

/** VNCRobot
 *
 * @author peter.quiring
 */

import java.awt.*;

public interface VNCRobot {
  public Rectangle getScreenSize();
  public int[] getScreenCapture();
  public void keyPress(int code);
  public void keyRelease(int code);
  public void mouseMove(int x, int y);
  public void mousePress(int button);
  public void mouseRelease(int button);
}
