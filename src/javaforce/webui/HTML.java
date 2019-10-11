package javaforce.webui;

/** HTML container for generic types
 *
 * @author pquiring
 */

public class HTML extends Container {
  private String tag, text;
  private boolean enclosed = true;

  public HTML(String tag) {
    this.tag = tag;
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
