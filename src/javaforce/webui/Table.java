package javaforce.webui;

/** Table
 *
 * @author pquiring
 */

public class Table extends Container {
  private int width, height, cols, rows;
  private boolean border;
  public Table(int width, int height, int cols, int rows) {
    this.width = width;  //X
    this.height = height;  //Y
    this.cols = cols;  //X
    this.rows = rows;  //Y
    setSize();
  }
  private void setSize() {
    setWidth((width * cols) + "px");
    setHeight((height * rows) + "px");
  }
  public void setBorder(boolean state) {
    border = state;
    if (border) {
      addClass("border");
    } else {
      removeClass("border");
    }
  }
  private int get(Component comp, String key, int def) {
    Integer i = (Integer)comp.getProperty(key);
    if (i == null) return def;
    return i;
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<table" + getAttrs() + ">");
    for(int y=0;y<rows;y++) {
      sb.append("<tr style='height: " + height + "px;'>");
      for(int x=0;x<cols;x++) {
        Component c = _get(x,y);
        if (c == SPAN) continue;
        if (c != null) {
          int spanx = get(c, "spanx", 1);
          int spany = get(c, "spany", 1);
          sb.append("<td");
          if (border) {
            sb.append(" class='border'");
          }
          if (spanx > 1) {
            sb.append(" colspan=" + spanx);
            x += spanx-1;
          }
          if (spany > 1) {
            sb.append(" rowspan=" + spany);
            y += spany-1;
          }
          int w = width * spanx;
          int h = height * spany;
          sb.append(" style='width:" + w + "px; height:" + h + "px;'");
          sb.append(">");
          sb.append(c.html());
          sb.append("</td>");
        } else {
          sb.append("<td style='width: " + width + "px; height: " + height + "px;'></td>");
        }
      }
      sb.append("</tr>");
    }
    sb.append("</table>");
    return sb.toString();
  }
  private static Component SPAN = new Label("");
  private Component _get(int x,int y) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component c = get(a);
      int cx = get(c, "x", -1);
      int cy = get(c, "y", -1);
      if (cx == x && cy == y) return c;
      int spanx = get(c, "spanx", 1) - 1;
      int spany = get(c, "spany", 1) - 1;
      if (spanx > 0 || spany > 0) {
        if ((x >= cx && x <= cx + spanx) &&
         (y >= cy && y <= cy + spany)) {
          return SPAN;
        }
      }
    }
    return null;
  }
  public void add(Component comp, int x, int y) {
    comp.setProperty("x", x);
    comp.setProperty("y", y);
    add(comp);
  }
  public void add(Component comp, int x, int y, int spanx, int spany) {
    comp.setProperty("x", x);
    comp.setProperty("y", y);
    comp.setProperty("spanx", spanx);
    comp.setProperty("spany", spany);
    add(comp);
  }
  public void addRow() {
    rows++;
    setSize();
  }
  public void addCol() {
    cols++;
    setSize();
  }
  public Component get(int col,int row) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component c = get(a);
      int cx = get(c, "x", -1);
      int cy = get(c, "y", -1);
      int spanx = get(c, "spanx", 1) - 1;
      int spany = get(c, "spany", 1) - 1;
      if ((x >= cx && x <= cx + spanx) &&
       (y >= cy && y <= cy + spany)) {
        return c;
      }
    }
    return null;
  }
  public int getRows() {
    return rows;
  }
  public int getColumns() {
    return cols;
  }
}
