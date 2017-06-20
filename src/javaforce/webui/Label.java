package javaforce.webui;

/** Label.java
 *
 * Generic Label.
 *
 * @author pquiring
 */

public class Label extends TextComponent {
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
  public void updateText(String txt) {
    sendEvent("settext", new String[] {"text=" + text});
  }
}
