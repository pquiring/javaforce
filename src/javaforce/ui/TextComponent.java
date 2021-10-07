package javaforce.ui;

/**
 *
 * @author pquiring
 */

public class TextComponent extends Component {
  private Font font;

  public TextComponent() {
    font = Font.getSystemFont();
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
  }
}
