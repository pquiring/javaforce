package javaforce.webui;

import javaforce.JFLog;

/** Button.java
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends Component {
  private String text;
  private Image img;
  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource res) {
    JFLog.log("Button::res=" + res);
    img = new Image(res);
    setClass("button");
    setStyle("border", "0px");
    setStyle("padding", "0px");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<button" + getAttrs() + ">");
    if (img != null) {
      sb.append(img.html());
    }
    if (text != null) {
      sb.append(text);
    }
    sb.append("</button>");
    return sb.toString();
  }
  public void setText(String text) {
    this.text = text;
    sendEvent("settext", new String[] {"text=" + text});
  }
}
