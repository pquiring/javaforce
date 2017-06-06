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
  private Resource img;
  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource res) {
    img = res;
    setClass("button");
  }
  public String html() {
    if (img != null) {
      return "<img" + getAttrs() +  " src='/static/" + img.id + "'>";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<button" + getAttrs() + ">");
    sb.append(text);
    sb.append("</button>");
    return sb.toString();
  }
  public void setText(String text) {
    this.text = text;
    sendEvent("settext", new String[] {"text=" + text});
  }
}
