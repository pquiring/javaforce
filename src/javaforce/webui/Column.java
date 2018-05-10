package javaforce.webui;

/** Column - display components in a column.
 *
 * @author pquiring
 */

public class Column extends Container {
  public Column() {
    setClass("column");
  }
  public void add(Component comp) {
    add(count(), comp);
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("column-item");
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
    sb.append("<div class='column-end'></div>");
    sb.append("</div>");
    return sb.toString();
  }
}
