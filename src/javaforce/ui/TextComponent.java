package javaforce.ui;

/**
 *
 * @author pquiring
 */

public class TextComponent extends Component {
  private Font font;
  private String text;

  public TextComponent() {
    font = Font.getSystemFont();
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
  }

  public String getText() {return text;}

  public void setText(String text) {
    this.text = text;
  }
}
