package javaforce.webui;

/** TextField
 *
 * @author pquiring
 */

public class TextField extends Component {
  public String text;
  public TextField(String text) {
    this.text = text;
    addEvent("onchange", "onTextChange(this);");
    setClass("textfield");
  }
  public String html() {
    return "<input id='" + name + "'" + getEvents() + " class='" + cls + "' value='" + text + "'>";
  }
  public void setText(String txt) {
    text = txt;
    peer.sendEvent(name, "settext", new String[] {"text=" + text});
  }
  public String getText() {
    return text;
  }

  public void dispatchEvent(String event, String args[]) {
    if (event.equals("changed")) {
      int idx = args[0].indexOf("=");
      text = args[0].substring(idx+1);
      if (change != null) change.onChange(this);
    }
  }

  private Change change;
  public void addChangeListener(Change handler) {
    change = handler;
  }
  public static interface Change {
    public void onChange(TextField combo);
  }
}
