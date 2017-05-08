package javaforce.webui;

/** Button.java
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends Component {
  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public String html() {
    return "<button" + getAttrs() + ">" + text + "</button>";
  }
  private String text;
  public void setText(String text) {
    this.text = text;
    sendEvent("settext", new String[] {"text=" + text});
  }
}
