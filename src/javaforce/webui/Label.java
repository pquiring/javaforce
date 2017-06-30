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
    setDisplay("table-cell");  //allows vertical-align (this is a block element so it needs to be placed in an inline-block div)
    setVerticalAlign(CENTER);
    addEvent("onclick", "onClick(event, this);");
    setClass("label");
    addClass("noselect");
    addClass("valigncenter");
  }
  public String html() {
    return "<div style='display:inline-block;'><div" + getAttrs() + ">" + text + "</div></div>";
  }
  public void updateText(String txt) {
    sendEvent("settext", new String[] {"text=" + text});
  }
}
