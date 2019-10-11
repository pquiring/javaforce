package javaforce.webui;

/** Row - display components in a row.
 *
 * @author pquiring
 */

public class Row extends Container {
  public Pad end;
  public Row() {
    addClass("row");
    end = new Pad();
    end.addClass("row-end");
    super.add(end);
  }
  public void add(Component comp) {
    add(count() - 1, comp);
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("row-item");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}
