package javaforce.webui;

/** Column - display components in a column.
 *
 * @author pquiring
 */

public class Column extends Container {
  public Column() {
    setClass("column");
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
