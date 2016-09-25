package javaforce.webui;

/** Panel to display components.
 *
 * @author pquiring
 */

public class Panel extends Container {
  public Panel() {
    setClass("panel column");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div id='" + id + "' class='" + cls + "'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}
