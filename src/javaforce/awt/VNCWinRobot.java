package javaforce.awt;

/** VNCWinRobot
 *
 * @author peter.quiring
 */

import java.awt.*;

import javaforce.jni.*;

public class VNCWinRobot implements VNCRobot {

  public VNCWinRobot() {
    WinNative.setStationDesktop("winsta0", "default");
  }

  public Rectangle getScreenSize() {
    int[] dim = new int[4];
    WinNative.getDesktopRect(dim);
    return new Rectangle(dim[0], dim[1], dim[2], dim[3]);
  }

  public int[] getScreenCapture() {
    int[] px = WinNative.getScreenCapture();
    return px;
  }

  public void keyPress(int code) {
    WinNative.simulateKeyDown(code);
  }

  public void keyRelease(int code) {
    WinNative.simulateKeyUp(code);
  }

  public void mouseMove(int x, int y) {
    WinNative.simulateMouseMove(x, y);
  }

  public void mousePress(int button) {
    WinNative.simulateMouseDown(button);
  }

  public void mouseRelease(int button) {
    WinNative.simulateMouseUp(button);
  }
}
