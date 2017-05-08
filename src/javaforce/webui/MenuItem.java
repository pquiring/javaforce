package javaforce.webui;

/** MenuItem
 *
 * @author pquiring
 */

public class MenuItem extends Container {
  public String text;
  public MenuItem(String text) {
    this.text = text;
    addEvent("onclick", "onClick(event, this);");
    addEvent("onmousedown", "onMouseDown(event, this);");
    addClass("menuitem");
    addClass("noselect");
  }
  public String html() {
    return "<div" + getAttrs() + ">" + text + "</div>";
  }
  public void setText(String txt) {
    text = txt;
    sendEvent("settext", new String[] {"text=" + text});
  }
}
