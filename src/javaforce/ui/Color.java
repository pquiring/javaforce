package javaforce.ui;

/** Color - ARGB
 *
 * @author pquiring
 */

public class Color {
  private int clr;  //ARGB

  public static int OPAQUE = 0xff000000;
  public static int MASK_ALPHA = 0xff000000;
  public static int MASK_RGB = 0x00ffffff;
  public static int MASK_RED = 0x00ff0000;
  public static int MASK_GREEN = 0x0000ff00;
  public static int MASK_BLUE = 0x000000ff;

  public static int black = 0x000000;
  public static int red = 0xff0000;
  public static int green = 0x00ff00;
  public static int blue = 0x0000ff;
  public static int white = 0xffffff;

  public static Color BLACK = new Color(black);
  public static Color RED = new Color(red);
  public static Color GREEN = new Color(green);
  public static Color BLUE = new Color(blue);
  public static Color WHITE = new Color(white);

  public Color(int clr) {
    this.clr = clr;
  }
  public Color(int r, int g, int b) {
    clr = OPAQUE + r << 16 + g << 8 + b;
  }
  public Color(int a, int r, int g, int b) {
    clr = a << 24 + r << 16 + g << 8 + b;
  }

  public int getColor() {
    return clr;
  }
}
