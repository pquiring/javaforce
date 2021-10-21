package javaforce.ui;

/** Component with text content.
 *
 * @author pquiring
 */

public class TextComponent extends FontComponent {
  private String text;

  public String getText() {return text;}

  public void setText(String text) {
    this.text = text;
  }
}
