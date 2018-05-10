package javaforce.webui;

/** Row - display components in a row.
 *
 * @author pquiring
 */

public class Row extends Container {
  public Row() {
    addClass("row");
  }
  public void add(Component comp) {
    super.add(comp);
    comp.addClass("row-item");
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("row-item");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    if (cnt == 0) {
      sb.append("&nbsp;");
    }
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("<div class='row-end'></div>");
    sb.append("</div>");
    return sb.toString();
  }
}
