package javaforce.webui;

/** TextArea
 *
 * @author pquiring
 */

public class TextArea extends TextComponent {
  public TextArea(String text) {
    this.text = text;
    addEvent("onchange", "onTextChange(event, this);");
    addEvent("onkeyup", "onTextChange(event, this);");
    setClass("textfield");
  }
  public String html() {
    return "<textarea" + getAttrs() + ">" + text + "</textarea>";
  }
  public void updateText(String txt) {
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void onChanged(String[] args) {
    int idx = args[0].indexOf("=");
    text = destringify(args[0].substring(idx+1));
    super.onChanged(args);
  }
}
