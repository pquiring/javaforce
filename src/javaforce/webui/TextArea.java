package javaforce.webui;

/** TextArea
 *
 * @author pquiring
 */

public class TextArea extends Component {
  public String text;
  public TextArea(String text) {
    this.text = text;
    addEvent("onchange", "onTextChange(this);");
    setClass("textfield");
  }
  public String html() {
    return "<textarea id='" + id + "'" + getEvents() + " class='" + cls + "'>" + text + "</textarea>";
  }
  public void setText(String txt) {
    text = txt;
    peer.sendEvent(id, "settext", new String[] {"text=" + text});
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
    public void onChange(TextArea combo);
  }
}
