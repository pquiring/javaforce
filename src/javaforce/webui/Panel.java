package javaforce.webui;

/** Panel to display components.
 *
 * @author pquiring
 */

public class Panel extends Container {
  public Panel() {
    addClass("panel");
    addClass("column");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + "'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}
