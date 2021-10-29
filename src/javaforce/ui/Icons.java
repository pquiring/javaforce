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

  public static Image getArrowUp() {
    if (arrow_up == null) {
      arrow_up = loadImage("/javaforce/icons/16/icon_up.png");
    }
    return arrow_up;
  }

  public static Image getArrowDown() {
    if (arrow_down == null) {
      arrow_down = loadImage("/javaforce/icons/16/icon_down.png");
    }
    return arrow_down;
  }

  public static Image getArrowLeft() {
    if (arrow_left == null) {
      arrow_left = loadImage("/javaforce/icons/16/icon_left.png");
    }
    return arrow_left;
  }

  public static Image getArrowRight() {
    if (arrow_right == null) {
      arrow_right = loadImage("/javaforce/icons/16/icon_right.png");
    }
    return arrow_right;
  }
}
