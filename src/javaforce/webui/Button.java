package javaforce.webui;

/** Button
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends TextComponent {
  private String url;
  private Resource img;
  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource res) {
    img = res;
    setClass("button");
  }
  public String html() {
    if (img != null) {
      return "<img" + getAttrs() +  " src='/static/" + img.id + "'>";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<button" + getAttrs() + ">");
    sb.append(text);
    sb.append("</button>");
    return sb.toString();
  }
  public void updateText(String text) {
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void setURL(String url) {
    addEvent("onclick", "window.open(\"" + url + "\");");
    this.url = url;
  }
  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=/static/" + img.id});
  }
}
