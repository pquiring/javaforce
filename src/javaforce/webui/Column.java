package javaforce.webui;

/** Column - display components in a column.
 *
 * @author pquiring
 */

public class Column extends Container {
  public Pad end;
  public Column() {
    setClass("column");
    end = new Pad();
    end.addClass("column-end");
    super.add(end);
  }
  public void add(Component comp) {
    add(count() - 1, comp);
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("column-item");
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
