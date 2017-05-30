package javaforce.webui;

/** Panel with scroll bars that appear automatically as needed.
 *
 * @author pquiring
 */

public class AutoScrollPanel extends Container {
  public AutoScrollPanel() {
    setClass("autoscroll");
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
