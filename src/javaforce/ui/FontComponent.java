package javaforce.ui;

/** Component with a font.
 *
 * @author pquiring
 */

public class FontComponent extends Component {
  private Font font;

  public FontComponent() {
    font = Font.getSystemFont();
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
  }

}
