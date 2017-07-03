package javaforce.webui;

/** Label.java
 *
 * Generic Label.
 *
 * @author pquiring
 */

public class Label extends TextComponent {
  private Resource img;
  public Label(String text) {
    this.text = text;
    setDisplay("table-cell");  //allows vertical-align (this is a block element so it needs to be placed in an inline-block div)
    setVerticalAlign(CENTER);
    addEvent("onclick", "onClick(event, this);");
    setClass("label");
    addClass("noselect");
  }
  public Label(Resource img) {
    this.img = img;
    addEvent("onclick", "onClick(event, this);");
    setClass("label");
    addClass("noselect");
  }
  public String html() {
    if (img != null) {
      return "<img" + getAttrs() +  " src='/static/" + img.id + "'>";
    }
    return "<div style='display:inline-block;'><div" + getAttrs() + ">" + text + "</div></div>";
  }
  public void updateText(String txt) {
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=/static/" + img.id});
  }
}
