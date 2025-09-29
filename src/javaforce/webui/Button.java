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
  private Icon icon;

  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource img) {
    this.img = new Image(img);
    add(this.img);
    setClass("button");
  }
  public Button(Icon img) {
    this.icon = icon;
    add(this.icon);
    setClass("button");
  }
  public Button(Resource img, String text) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setClass("button");
  }
  public Button(Icon img, String text) {
    this.icon = icon;
    add(this.icon);
    this.text = text;
    setClass("button");
  }
  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<button" + getAttrs() + ">");
    if (img != null) {
      html.append(img.html());
    }
    if (icon != null) {
      html.append(icon.html());
    }
    if (text != null) {
      html.append(text);
    }
    html.append("</button>");
    return html.toString();
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
