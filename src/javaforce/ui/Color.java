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
  public static int light_red = 0xee9090;
  public static int dark_red = 0x770000;
  public static int red = 0xff0000;
  public static int light_green = 0x90ee90;
  public static int dark_green = 0x007700;
  public static int green = 0x00ff00;
  public static int light_blue = 0x9090ee;
  public static int dark_blue = 0x000077;
  public static int blue = 0x0000ff;
  public static int light_gray = 0x444444;
  public static int gray = 0x777777;
  public static int dark_gray = 0xcccccc;
  public static int white = 0xffffff;

  //composites
  public static int yellow = 0xffff00;
  public static int magenta = 0xff00ff;
  public static int cyan = 0x00ffff;

  //common
  public static int orange = 0xffa500;
  public static int purple = 0x800080;
  public static int brown = 0xa52a2a;
  public static int pink = 0xffc0cb;

  public static Color BLACK = new Color(black);
  public static Color LIGHT_RED = new Color(light_red);
  public static Color DARK_RED = new Color(dark_red);
  public static Color RED = new Color(red);
  public static Color LIGHT_GREEN = new Color(light_green);
  public static Color DARK_GREEN = new Color(dark_green);
  public static Color GREEN = new Color(green);
  public static Color LIGHT_BLUE = new Color(light_blue);
  public static Color DARK_BLUE = new Color(dark_blue);
  public static Color BLUE = new Color(blue);
  public static Color LIGHT_GRAY = new Color(light_gray);
  public static Color GRAY = new Color(gray);
  public static Color DARK_GRAY = new Color(dark_gray);
  public static Color WHITE = new Color(white);

  public static Color YELLOW = new Color(yellow);
  public static Color MAGENTA = new Color(magenta);
  public static Color CYAN = new Color(cyan);

  public static Color ORANGE = new Color(orange);
  public static Color PURPLE = new Color(purple);
  public static Color BROWN = new Color(brown);
  public static Color PINK = new Color(pink);

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

  public String toString() {
    return "Color:0x" + Integer.toString(clr, 16);
  }
}
