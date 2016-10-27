package javaforce.webui;

/** MenuBar
 *
 * @author pquiring
 */

public class MenuBar extends Container {
  public MenuBar() {
    addClass("menubar");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}
