package javaforce.ui;

/** Check Box
 *
 * @author pquiring
 */

public class CheckBox extends ToggleButton {
  public CheckBox(String text) {
    super(text);
  }
  public Dimension getMinSize() {
    FontMetrics metrics = getFont().getMetrics(getText());
    int w = metrics.getWidth() + 16 + 8;
    int h = metrics.getHeight();
    if (h < 16) {
      h = 16;
    }
    h += 8;
    return new Dimension(w, h);
  }
  public void render(Image image) {
    int x = pos.x;
    int y = pos.y;
    int w = size.width;
    int h = size.height;
    if (w == 0 || h == 0) return;
    if (isEnabled()) {
      image.setForeColor(getForeColor());
    } else {
      image.setForeColor(getDisabledColor());
    }
    image.setLineStyle(LineStyle.SOLID);
    y += (getHeight() - 16) / 2;
    //draw box
    image.drawBox(x + 0, y + 0, 16, 16);
    //draw check mark
    if (isSelected()) {
      image.drawLine(x + 0, y + 8 , x + 7 , y + 15);
      image.drawLine(x + 7, y + 15, x + 15, y + 0);
    }
    renderText(image, 20, 4);
  }
}
