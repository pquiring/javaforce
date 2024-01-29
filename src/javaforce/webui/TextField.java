package javaforce.webui;

/** TextField
 *
 * @author pquiring
 */

public class TextField extends TextComponent {
  public boolean password;
  public TextField(String text) {
    this.text = text;
    addEvent("onchange", "onTextChange(event, this);");
    setClass("textfield");
  }
  public String html() {
    return "<input" + getAttrs() + " value='" + text + "'>";
  }
  public void updateText(String txt) {
    sendEvent("setvalue", new String[] {"value=" + text});
  }
  public void setPassword(boolean state) {
    if (state)
      addAttr("type", "password");
    else
      removeAttr("type");
  }
  public void onChanged(String[] args) {
    int idx = args[0].indexOf("=");
    text = destringify(args[0].substring(idx+1));
    super.onChanged(args);
  }
}
