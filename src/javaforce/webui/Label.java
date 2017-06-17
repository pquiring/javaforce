package javaforce.webui;

/** Label.java
 *
 * Generic Label.
 *
 * @author pquiring
 */

public class Label extends Component {
  public String text;
  public Label(String text) {
    this.text = text;
    addEvent("onclick", "onClick(event, this);");
    setClass("label");
    addClass("noselect");
    addClass("valigncenter");
  }
  public String html() {
    return "<label" + getAttrs() + ">" + text + "</label>";
  }
  public void setText(String txt) {
    text = txt;
    sendEvent("settext", new String[] {"text=" + text});
  }
  public String getText() {
    return text;
  }
}
