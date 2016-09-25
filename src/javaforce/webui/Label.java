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
    addEvent("onclick", "click(this);");
    setClass("noselect");
  }
  public String html() {
    return "<label id='" + name + "'" + getEvents() + " class='" + cls + "'>" + text + "</label>";
  }
  public void setText(String txt) {
    text = txt;
    peer.sendEvent(name, "settext", new String[] {"text=" + text});
  }
}
