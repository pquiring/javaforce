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
    addEvent("onclick", "onClick(event, this);");
    setClass("button");
  }
  public String html() {
    return "<button" + getAttrs() + ">" + text + "</button>";
  }
  private String text;
  public void setText(String text) {
    this.text = text;
  }
}
