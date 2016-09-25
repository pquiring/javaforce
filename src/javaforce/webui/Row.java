package javaforce.webui;

/** Row - display components in a row.
 *
 * @author pquiring
 */

public class Row extends Container {
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div id='" + id + "' class='row'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}
