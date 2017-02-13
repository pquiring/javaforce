package javaforce.webui;

/** ToggleButton.java
 *
 * Toggle button.
 *
 * @author pquiring
 */

public class ToggleButton extends Component {
  private boolean state;
  public ToggleButton(String text) {
    this.text = text;
    addEvent("onclick", "onClick(event, this);");
    setClass("toggle_off");
  }
  public void onClick(String args[]) {
    state = !state;
    getClient().sendEvent(id, "setclass", new String[] {"cls=" + (state ? "toggle_on" : "toggle_off")});
  }
  public String html() {
    return "<button" + getAttrs() + ">" + text + "</button>";
  }
  private String text;
  public void setText(String text) {
    this.text = text;
    getClient().sendEvent(id, "settext", new String[] {"text=" + text});
  }
  public void setSelected(boolean state) {
    this.state = state;
    getClient().sendEvent(id, "setclass", new String[] {"cls=" + (state ? "toggle_on" : "toggle_off")});
  }
  public boolean isSelected() {
    return state;
  }
}
