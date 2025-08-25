package javaforce.webui;

/** Button
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends TextComponent {
  private String url;
  private Image img;
  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource img) {
    this.img = new Image(img);
    add(this.img);
    setClass("button");
  }
  public Button(Resource img, String text) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setClass("button");
  }
  public String html() {
    if (img != null && text == null) {
      return img.html();
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<button" + getAttrs() + ">");
    if (img != null) {
      sb.append(img.html());
    }
    if (text != null) {
      sb.append(text);
    }
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
  public String getURL() {
    return url;
  }
  public void setImage(Resource img) {
    this.img.setImage(img);
  }
}
