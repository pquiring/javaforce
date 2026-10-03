package javaforce.webui;

/** HTML container for generic types
 *
 * @author pquiring
 */

public class HTMLContainer extends Container {
  private String text;
  private boolean enclosed = true;

  public HTMLContainer(String tag) {
    setTag(tag);
    if (tag.equals("hr") || tag.equals("br")) {
      enclosed = false;
    }
    text = "";
  }

  public HTMLContainer(String tag, String text) {
    setTag(tag);
    this.text = text;
  }

  public void setEnclosed(boolean state) {
    this.enclosed = state;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<" + getTag() + getAttrs() + ">");
    int cnt = count();
    if (cnt == 0) {
      html.append(text);
    } else {
      for(int a=0;a<cnt;a++) {
        html.append(get(a).html());
      }
    }
    if (enclosed) html.append("</" + getTag() + ">");
    return html.toString();
  }

}
