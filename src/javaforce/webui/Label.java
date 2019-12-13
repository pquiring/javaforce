package javaforce.webui;

/** Label
 *
 * Generic Label.
 *
 * @author pquiring
 */

public class Label extends TextComponent {
  private Resource img;
  public Label(String text) {
    this.text = text;
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
    return "<div" + getAttrs() + ">" + text + "</div>";
  }
  public void updateText(String txt) {
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=/static/" + img.id});
  }
}
