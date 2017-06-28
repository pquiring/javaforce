package javaforce.webui;

/** WebUI Color
 *
 * @author pquiring
 */

public class Color {
  public int r,g,b;  //0-255

  public static int white = 0xffffff;
  public static int darkGrey = 0xeeeeee;
  public static int grey = 0xcccccc;
  public static int lightGrey = 0xaaaaaa;
  public static int black = 0x000000;

  public static int red = 0xff0000;
  public static int green = 0x00ff00;
  public static int blue = 0x0000ff;

  public static int darkRed = 0xcc0000;
  public static int darkGreen = 0x00cc00;
  public static int darkBlue = 0x0000cc;

  public Color(int rgb) {
    setRGB(rgb);
  }
  public int getRGB() {
    return r | g << 8 | b << 16;
  }
  public void setRGB(int rgb) {
    r = rgb & 0xff;
    g = (rgb >> 8) & 0xff;
    b = (rgb >> 16) & 0xff;
  }
  public String toString() {
    return String.format("#%02d%02d%02d", r, g, b);
  }
}
