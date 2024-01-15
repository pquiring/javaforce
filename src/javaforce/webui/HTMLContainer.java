package javaforce.webui;

/** HTML container for generic types
 *
 * @author pquiring
 */

public class HTMLContainer extends Container {
  private String tag, text;
  private boolean enclosed = true;

  public HTMLContainer(String tag) {
    this.tag = tag;
    if (tag.equals("hr") || tag.equals("br")) {
      enclosed = false;
    }
    text = "";
  }

  public HTMLContainer(String tag, String text) {
    this.tag = tag;
    this.text = text;
  }

  public void setEnclosed(boolean state) {
    this.enclosed = state;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<" + tag + getAttrs() + ">");
    int cnt = count();
    if (cnt == 0) {
      sb.append(text);
    } else {
      for(int a=0;a<cnt;a++) {
        sb.append(get(a).html());
      }
    }
    if (enclosed) sb.append("</" + tag + ">");
    return sb.toString();
  }

}
