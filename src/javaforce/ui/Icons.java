package javaforce.ui;

/** Icon Resources
 *
 * @author pquiring
 */

import javaforce.*;

public class Icons {
  private static Image arrow_up;
  private static Image arrow_down;
  private static Image arrow_left;
  private static Image arrow_right;

  public static Image loadImage(String name) {
    try {
      Image image = new Image();
      if (!image.loadPNG(new JF().getClass().getResourceAsStream(name))) {
        throw new Exception("Load resource failed:" + name);
      }
      return image;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static Image loadIcon(String name) {
    return loadImage("/javaforce/icons/16/" + name + ".png");
  }

  public static Image getArrowUp() {
    if (arrow_up == null) {
      arrow_up = loadIcon("up-arrow");
    }
    return arrow_up;
  }

  public static Image getArrowDown() {
    if (arrow_down == null) {
      arrow_down = loadIcon("down-arrow");
    }
    return arrow_down;
  }

  public static Image getArrowLeft() {
    if (arrow_left == null) {
      arrow_left = loadIcon("left-arrow");
    }
    return arrow_left;
  }

  public static Image getArrowRight() {
    if (arrow_right == null) {
      arrow_right = loadIcon("right-arrow");
    }
    return arrow_right;
  }
}
