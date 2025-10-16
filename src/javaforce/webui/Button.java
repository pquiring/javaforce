package javaforce.webui;

/** Button
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends TextComponent {
  protected String url;
  protected Image img;
  protected Icon icon;

  public Button(String text) {
    this.text = text;
    setClass("button");
  }
  public Button(Resource img) {
    this.img = new Image(img);
    add(this.img);
    setClass("button");
  }
  public Button(Icon icon) {
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
  public Button(Icon icon, String text) {
    this.icon = icon;
    add(this.icon);
    this.text = text;
    setClass("button");
  }

  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<button" + getAttrs() + ">");
    html.append(innerHTML());
    html.append("</button>");
    return html.toString();
  }
  public String innerHTML() {
    StringBuilder html = new StringBuilder();
    if (img != null) {
      html.append(img.html());
    }
    if (icon != null) {
      html.append(icon.html());
    }
    if (text != null) {
      html.append(text);
    }
    return html.toString();
  }
  public void update() {
    sendEvent("sethtml", new String[] {"html=" + innerHTML()});
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
  public void setIcon(Icon icon) {
    this.icon = icon;
    update();
  }
}
