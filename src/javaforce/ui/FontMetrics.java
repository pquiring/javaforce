package javaforce.ui;

/** FontMetrics
 *
 * @author pquiring
 */

public class FontMetrics {
  public int ascent;
  public int descent;
  public int advance;

  public FontMetrics() {
  }

  public int getWidth() {
    return advance;
  }

  public void setAscent(int ascent) {
    this.ascent = ascent;
  }

  public int getHeight() {
    return -ascent + descent;
  }

  public void setDescent(int descent) {
    this.descent = descent;
  }

  public int getBaseline() {
    return -ascent;
  }

  public void setBaseline(int baseline) {
    this.ascent = baseline;
  }

  public Dimension toDimension() {
    return new Dimension(getWidth(), getHeight());
  }
}
