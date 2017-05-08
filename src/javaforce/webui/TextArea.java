package javaforce.webui;

/** TextArea
 *
 * @author pquiring
 */

public class TextArea extends Component {
  public String text;
  public TextArea(String text) {
    this.text = text;
    addEvent("onchange", "onTextChange(event, this);");
    setClass("textfield");
  }
  public String html() {
    return "<textarea" + getAttrs() + ">" + text + "</textarea>";
  }
  public void setText(String txt) {
    text = txt;
    sendEvent("settext", new String[] {"text=" + text});
  }
  public String getText() {
    return text;
  }

  public void onChanged(String args[]) {
    int idx = args[0].indexOf("=");
    text = args[0].substring(idx+1);
  }
}
