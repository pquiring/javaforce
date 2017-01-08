package javaforce.webui;

/** WebUI Color
 *
 * @author pquiring
 */

public class Color {
  public int r,g,b;  //0-255
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
}
