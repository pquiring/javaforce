package javaforce.webui;

/** Text Component
 *
 * @author pquiring
 */

public abstract class TextComponent extends Component {
  protected String text;
  public abstract void updateText(String text);
  public void setText(String text) {
    this.text = text;
    updateText(text);
    onChanged(new String[] {"text=" + text});
 }
  public String getText() {
    return text;
  }
}
